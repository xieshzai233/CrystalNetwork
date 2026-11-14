package dev.crystalnetwork.ported.blocks.energynet.tileentity;

import codechicken.lib.data.MCDataInput;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.lib.ChatHelper;
import com.brandon3055.brandonscore.lib.Vec3B;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.lib.datamanager.DataFlags;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.brandon3055.brandonscore.utils.EnergyUtils;
import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandler;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandlerClientWireless;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandlerServerWireless;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXRing;
import dev.crystalnetwork.CrystalNetwork;
import dev.crystalnetwork.ported.init.CrystalContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class TileCrystalWirelessIO extends TileCrystalBase {
   protected Map<Vec3B, Direction> receiverSideMap = new HashMap<>();
   protected LinkedList<Vec3B> linkedReceivers = new LinkedList<>();
   protected LinkedList<BlockPos> receiverCache = null;
   protected Map<BlockPos, Direction> receiverFaceCache = null;
   protected List<TileCrystalWirelessIO.LinkedDevice> fastList = new ArrayList<>();
   protected List<TileCrystalWirelessIO.LinkedDevice> slowList = new ArrayList<>();
   public LinkedList<int[]> receiverTransferRates = new LinkedList<>();
   public LinkedList<Byte> receiverFlowRates = new LinkedList<>();
   public final ManagedBool useUpdateOptimisation = (ManagedBool)this.dataManager
      .register(new ManagedBool("transport_state", true, new DataFlags[]{DataFlags.SAVE_BOTH_SYNC_CONTAINER}));
   public final ManagedBool inputMode = (ManagedBool)this.dataManager.register(new ManagedBool("input_mode", new DataFlags[]{DataFlags.SAVE_BOTH_SYNC_TILE}));
   public Map<Integer, Integer> containerReceiverFlow = new HashMap<>();

   public TileCrystalWirelessIO(BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_WIRELESS_CRYSTAL.get(), pos, state);
   }

   public TileCrystalWirelessIO(TechLevel techLevel, BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_WIRELESS_CRYSTAL.get(), techLevel, pos, state);
   }

   public static void register(RegisterCapabilitiesEvent event) {
      energyCapability(event, CrystalContent.TILE_WIRELESS_CRYSTAL);
   }

   @Override
   public void tick() {
      if (!this.level.isClientSide) {
         this.updateEnergyFlow();
      }

      super.tick();
   }

   private void updateEnergyFlow() {
      if (this.receiverTransferRates.size() != this.linkedReceivers.size()) {
         this.rebuildReceiverTransferList();
      }

      this.getReceiversFaces();
      List<TileCrystalWirelessIO.LinkedDevice> moveToSlow = new ArrayList<>();

      for (TileCrystalWirelessIO.LinkedDevice receiver : this.fastList) {
         if (!this.updateDevice(receiver)) {
            this.removeReceiver(receiver.pos);
            return;
         }

         if (receiver.timeOut > 40) {
            moveToSlow.add(receiver);
         }
      }

      if (this.tick % 80 == 0) {
         List<TileCrystalWirelessIO.LinkedDevice> moveToFast = new ArrayList<>();

         for (TileCrystalWirelessIO.LinkedDevice receiver : this.slowList) {
            if (!this.updateDevice(receiver)) {
               this.removeReceiver(receiver.pos);
               return;
            }

            if (receiver.timeOut == 0) {
               moveToFast.add(receiver);
            }
         }

         if (!moveToFast.isEmpty()) {
            this.fastList.addAll(moveToFast);
            this.slowList.removeAll(moveToFast);
         }
      }

      if (!moveToSlow.isEmpty()) {
         this.slowList.addAll(moveToSlow);
         this.fastList.removeAll(moveToSlow);
      }

      if (!this.level.isClientSide && CrystalNetwork.serverTicks % 10 == 0) {
         this.receiverFlowRates.clear();

         for (int i = 0; i < this.linkedReceivers.size(); i++) {
            this.receiverFlowRates.add(this.flowConversion(this.receiverTransfer(i)));
         }
      }
   }

   public InteractionResult useWithoutItem(BlockState state, Player player, BlockHitResult hit) {
      if (player.isShiftKeyDown()) {
         this.inputMode.invert();
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.FAIL;
      }
   }

   protected boolean updateDevice(TileCrystalWirelessIO.LinkedDevice receiver) {
      if (!receiver.isLinkValid(this.level)) {
         return receiver.invalidTime++ < 100;
      } else {
         receiver.invalidTime = 0;
         BlockEntity tile = receiver.getCachedTile();
         if (tile == null) {
            if (receiver.timeOut < 40) {
               receiver.timeOut = 40;
            } else {
               receiver.timeOut++;
            }

            return true;
         } else {
            long transferred;
            if (this.inputMode.get()) {
               transferred = EnergyUtils.extractEnergy(tile, (long)this.opStorage.receiveEnergy(this.getMaxWirelessTransfer(), true), receiver.side, false);
               this.opStorage.receiveOP(transferred, false);
            } else {
               transferred = EnergyUtils.insertEnergy(tile, (long)this.opStorage.extractEnergy(this.getMaxWirelessTransfer(), true), receiver.side, false);
               this.opStorage.extractOP(transferred, false);
            }

            if (transferred > 0L) {
               receiver.timeOut = 0;
            } else {
               receiver.timeOut++;
            }

            this.receiverTransferRates.get(receiver.index)[this.tick % 20] = (int)Math.min(transferred, 2147483647L);
            return true;
         }
      }
   }

   public void rebuildReceiverTransferList() {
      this.receiverTransferRates.clear();
      this.receiverFlowRates.clear();

      for (int i = 0; i < this.linkedReceivers.size(); i++) {
         this.receiverTransferRates.add(new int[20]);
         this.receiverFlowRates.add((byte)0);
      }
   }

   public int getMaxWirelessTransfer() {
      return this.getTier() == 0 ? 32000 : (this.getTier() == 1 ? 128000 : 512000);
   }

   public int receiverTransfer(int index) {
      long sum = 0L;

      for (int transfer : this.receiverTransferRates.get(index)) {
         sum += (long)transfer;
      }

      return (int)(sum / 20L);
   }

   public byte flowConversion(int transferRate) {
      double d = (double)transferRate / ((double)this.getMaxWirelessTransfer() * 0.01 + (double)transferRate);
      return (byte)((int)(d * 255.0));
   }

   @Override
   public boolean binderUsed(Player player, BlockPos linkTarget, Direction sideClicked) {
      BlockEntity tile = this.level.getBlockEntity(linkTarget);
      if (tile == null || tile instanceof ICrystalLink) {
         return super.binderUsed(player, linkTarget, sideClicked);
      } else if (this.level.isClientSide) {
         return true;
      } else {
         Vec3B offset = this.getOffset(linkTarget);
         if (this.linkedReceivers.contains(offset)) {
            this.removeReceiver(linkTarget);
            ChatHelper.sendIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.link_broken").withStyle(ChatFormatting.GREEN), MSG_ID);
            return true;
         } else {
            if (this.inputMode.get()) {
               if (!EnergyUtils.canExtractEnergy(tile, sideClicked)) {
                  if (EnergyUtils.getStorage(tile, sideClicked) != null) {
                     ChatHelper.sendIndexed(
                        player, Component.translatable("gui.crystalnetwork.energy_net.side_can_not_extract").withStyle(ChatFormatting.RED), MSG_ID
                     );
                     return false;
                  }

                  return super.binderUsed(player, linkTarget, sideClicked);
               }
            } else if (!EnergyUtils.canReceiveEnergy(tile, sideClicked)) {
               if (EnergyUtils.getStorage(tile, sideClicked) != null) {
                  ChatHelper.sendIndexed(
                     player, Component.translatable("gui.crystalnetwork.energy_net.side_can_not_receive").withStyle(ChatFormatting.RED), MSG_ID
                  );
                  return false;
               }

               return super.binderUsed(player, linkTarget, sideClicked);
            }

            if (this.linkedReceivers.size() >= this.getMaxReceivers()) {
               ChatHelper.sendIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.max_receivers").withStyle(ChatFormatting.RED), MSG_ID);
               return false;
            } else {
               this.addReceiver(linkTarget, sideClicked);
               ChatHelper.sendIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.devices_linked").withStyle(ChatFormatting.GREEN), MSG_ID);
               return true;
            }
         }
      }
   }

   public List<BlockPos> getReceivers() {
      if (this.receiverCache == null || this.receiverCache.size() != this.linkedReceivers.size()) {
         this.reCachePositions();
      }

      return this.receiverCache;
   }

   public Map<BlockPos, Direction> getReceiversFaces() {
      if (this.receiverCache == null
         || this.receiverCache.size() != this.linkedReceivers.size()
         || this.receiverFaceCache == null
         || this.receiverFaceCache.size() != this.linkedReceivers.size()) {
         this.reCachePositions();
      }

      return this.receiverFaceCache;
   }

   public int getMaxReceivers() {
      return this.getTier() == 0 ? 16 : (this.getTier() == 1 ? 32 : 64);
   }

   private void reCachePositions() {
      this.receiverCache = new LinkedList<>();
      this.receiverFaceCache = new HashMap<>();
      this.fastList.clear();
      this.slowList.clear();

      for (Vec3B offset : this.linkedReceivers) {
         this.receiverCache.add(this.fromOffset(offset));
         this.receiverFaceCache.put(this.fromOffset(offset), this.receiverSideMap.get(offset));
         this.fastList
            .add(new TileCrystalWirelessIO.LinkedDevice(this.linkedReceivers.indexOf(offset), this.fromOffset(offset), this.receiverSideMap.get(offset)));
      }

      this.updateBlock();
   }

   @Override
   public EnergyCrystal.CrystalType getCrystalType() {
      return EnergyCrystal.CrystalType.WIRELESS;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public CrystalFXBase createStaticFX() {
      return new CrystalFXRing((ClientLevel)this.level, this);
   }

   @Override
   public Vec3D getBeamLinkPos(BlockPos linkTo) {
      Vec3D thisVec = Vec3D.getCenter(this.worldPosition);
      Vec3D targVec = Vec3D.getCenter(linkTo);
      double dist = thisVec.distXZ(targVec);
      double offM = 0.4;
      if (dist == 0.0) {
         return this.worldPosition.getY() > linkTo.getY() ? thisVec.subtract(0.0, 0.4, 0.0) : thisVec.subtract(0.0, -0.4, 0.0);
      } else {
         double xDist = thisVec.x - targVec.x;
         double zDist = thisVec.z - targVec.z;
         double xOff = xDist / dist;
         double zOff = zDist / dist;
         return thisVec.subtract(xOff * offM, 0.0, zOff * offM);
      }
   }

   @Override
   public boolean renderBeamTermination() {
      return true;
   }

   @Override
   public ENetFXHandler createServerFXHandler() {
      return new ENetFXHandlerServerWireless(this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public ENetFXHandler createClientFXHandler() {
      return new ENetFXHandlerClientWireless(this);
   }

   @Override
   public void addDisplayData(List<Component> displayList) {
      super.addDisplayData(displayList);
      displayList.add(
         Component.translatable("gui.crystalnetwork.energy_net.hud_wireless_links")
            .append(": " + this.getReceivers().size() + " / " + this.getMaxReceivers())
            .withStyle(ChatFormatting.GREEN)
      );
      ChatFormatting colour = !this.inputMode.get() ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA;
      displayList.add(
         Component.translatable("gui.crystalnetwork.energy_net.io_mode")
            .append(" ")
            .append(Component.translatable("gui.crystalnetwork.energy_net.io_output_" + !this.inputMode.get()).withStyle(colour))
      );
   }

   public void addReceiver(BlockPos pos, Direction side) {
      Vec3B offset = this.getOffset(pos);
      this.linkedReceivers.add(offset);
      this.receiverSideMap.put(offset, side);
      this.reCachePositions();
      this.updateBlock();
   }

   public void removeReceiver(BlockPos pos) {
      Vec3B offset = this.getOffset(pos);
      this.linkedReceivers.remove(offset);
      this.receiverSideMap.remove(offset);
      this.reCachePositions();
      this.updateBlock();
   }

   @Override
   public void writeExtraNBT(Provider provider, CompoundTag compound) {
      super.writeExtraNBT(provider, compound);
      ListTag list = new ListTag();

      for (Vec3B vec : this.linkedReceivers) {
         CompoundTag receiver = new CompoundTag();
         receiver.putByteArray("offset", new byte[]{vec.x, vec.y, vec.z});
         receiver.putByte("side", (byte)this.receiverSideMap.get(vec).get3DDataValue());
         list.add(receiver);
      }

      compound.put("linked_receivers", list);
   }

   @Override
   public void readExtraNBT(Provider provider, CompoundTag compound) {
      super.readExtraNBT(provider, compound);
      ListTag list = compound.getList("linked_receivers", 10);
      this.linkedReceivers.clear();
      this.receiverSideMap.clear();

      for (int i = 0; i < list.size(); i++) {
         CompoundTag receiver = list.getCompound(i);
         byte[] offset = receiver.getByteArray("offset");
         Vec3B vec = new Vec3B(offset[0], offset[1], offset[2]);
         this.linkedReceivers.add(vec);
         this.receiverSideMap.put(vec, Direction.from3DDataValue(receiver.getByte("side")));
      }

      this.receiverCache = null;
   }

   @Override
   public void detectAndSendContainerChanges(List<ContainerListener> listeners) {
      super.detectAndSendContainerChanges(listeners);
      if (this.linkedReceivers.size() != this.receiverTransferRates.size() && !this.level.isClientSide) {
         this.rebuildReceiverTransferList();
      }

      List<BlockPos> positions = this.getReceivers();
      ListTag list = new ListTag();

      for (BlockPos lPos : positions) {
         int index = positions.indexOf(lPos);
         if (!this.containerReceiverFlow.containsKey(index) || this.containerReceiverFlow.get(index) != this.receiverTransfer(index)) {
            this.containerReceiverFlow.put(index, this.receiverTransfer(index));
            CompoundTag data = new CompoundTag();
            data.putByte("I", (byte)index);
            data.putInt("E", this.receiverTransfer(index));
            list.add(data);
         }
      }

      CompoundTag compound = new CompoundTag();
      if (!list.isEmpty()) {
         compound.put("L", list);
         this.sendUpdateToListeners(listeners, this.sendPacketToClient(output -> output.writeCompoundNBT(compound), 1));
      } else if (this.containerReceiverFlow.size() > this.linkedReceivers.size()) {
         this.containerReceiverFlow.clear();
         this.sendUpdateToListeners(listeners, this.sendPacketToClient(output -> output.writeCompoundNBT(compound), 1));
      }
   }

   @Override
   public void receivePacketFromServer(MCDataInput data, int id) {
      super.receivePacketFromServer(data, id);
      if (id == 1) {
         CompoundTag compound = data.readCompoundNBT();
         ListTag list = compound.getList("L", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag tagData = list.getCompound(i);
            this.containerReceiverFlow.put(Integer.valueOf(tagData.getByte("I")), tagData.getInt("E"));
         }
      }
   }

   @Override
   public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
      super.receivePacketFromClient(data, client, id);
      if (id == 11) {
         int intValue = data.readInt();
         if (this.getReceivers().size() > intValue && intValue >= 0) {
            BlockPos target = this.getReceivers().get(intValue);
            this.removeReceiver(target);
         }
      } else if (id == 21) {
         for (BlockPos target : new ArrayList<>(this.getReceivers())) {
            this.removeReceiver(target);
         }
      }
   }

   private class LinkedDevice {
      public final int index;
      public final BlockPos pos;
      private Direction side;
      public int timeOut = 0;
      private BlockEntity tileCache = null;
      private int invalidTime = 0;

      public LinkedDevice(int index, BlockPos pos, Direction side) {
         this.index = index;
         this.pos = pos;
         this.side = side;
      }

      public boolean isLinkValid(Level world) {
         this.tileCache = world.getBlockEntity(this.pos);
         return this.tileCache != null && EnergyUtils.getStorage(this.tileCache, this.side) != null || !TileCrystalWirelessIO.this.level.isLoaded(this.pos);
      }

      public BlockEntity getCachedTile() {
         return this.tileCache;
      }
   }
}
