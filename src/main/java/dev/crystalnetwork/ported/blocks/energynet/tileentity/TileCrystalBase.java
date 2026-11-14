package dev.crystalnetwork.ported.blocks.energynet.tileentity;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.packet.PacketCustom;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.api.power.OPStorage;
import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.brandonscore.lib.ChatHelper;
import com.brandon3055.brandonscore.lib.IInteractTile;
import com.brandon3055.brandonscore.lib.ITilePlaceListener;
import com.brandon3055.brandonscore.lib.Vec3B;
import com.brandon3055.brandonscore.utils.MathUtils;
import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandler;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandlerClient;
import dev.crystalnetwork.ported.blocks.energynet.rendering.ENetFXHandlerServer;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.CrystalNetwork;
import dev.crystalnetwork.ported.CrystalNetworkProxy;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TileCrystalBase extends TileBCore implements ITilePlaceListener, ICrystalLink, IInteractTile, IENetEffectTile {
   public static final UUID MSG_ID = UUID.fromString("b0729ef4-3c3a-4339-bda1-8630f7395df6");
   private static Map<EnergyCrystal.CrystalType, int[]> MAX_LINKS = new HashMap<>();
   protected int tick = 0;
   protected LinkedList<Vec3B> linkedCrystals = new LinkedList<>();
   public LinkedList<int[]> transferRatesArrays = new LinkedList<>();
   public LinkedList<Byte> flowRates = new LinkedList<>();
   private LinkedList<BlockPos> linkedPosCache = null;
   protected OPStorage opStorage = new OPStorage(this, 0L);
   protected ENetFXHandler fxHandler;
   protected TechLevel techLevel;
   boolean hashCached = false;
   int hashID = 0;
   public Map<Integer, Integer> containerEnergyFlow = new HashMap<>();

   public TileCrystalBase(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
      this(tileEntityTypeIn, TechLevel.DRACONIUM, pos, state);
   }

   public TileCrystalBase(BlockEntityType<?> tileEntityTypeIn, TechLevel techLevel, BlockPos pos, BlockState state) {
      super(tileEntityTypeIn, pos, state);
      this.techLevel = techLevel;
      this.fxHandler = CrystalNetworkProxy.createENetFXHandler(this);
      this.capManager.setInternalManaged("energy", CapabilityOP.BLOCK, this.opStorage).saveBoth().syncContainer();
   }

   public void tick() {
      super.tick();
      if (this.linkedCrystals.size() != this.transferRatesArrays.size() && !this.level.isClientSide) {
         this.rebuildTransferList();
      }

      this.balanceLinkedDevices();
      this.fxHandler.update();
      if (!this.level.isClientSide && CrystalNetwork.serverTicks % 10 == 0) {
         this.flowRates.clear();

         for (int i = 0; i < this.linkedCrystals.size(); i++) {
            this.flowRates.add(this.calculateFlow(i));
         }

         this.fxHandler.detectAndSendChanges();
      }

      this.tick++;
   }

   public void balanceLinkedDevices() {
      if (!this.level.isClientSide) {
         for (BlockPos linkedPos : this.getLinks()) {
            BlockEntity linkedTile = this.level.getBlockEntity(linkedPos);
            if (!(linkedTile instanceof ICrystalLink)) {
               if (this.level.hasChunkAt(linkedPos)) {
                  this.breakLink(linkedPos);
                  return;
               }
            } else {
               ICrystalLink linkedCrystal = (ICrystalLink)linkedTile;
               double thisCap = (double)this.getEnergyStored() / (double)this.getMaxEnergyStored();
               double thatCap = (double)linkedCrystal.getEnergyStored() / (double)linkedCrystal.getMaxEnergyStored();
               double diff = thisCap - thatCap;
               if (linkedCrystal.balanceMode() == 0 && thatCap < 1.0) {
                  this.transferRatesArrays.get(this.linkedPosCache.indexOf(linkedPos))[this.tick % 20] = this.balanceTransfer(linkedCrystal, 1.0 - thatCap);
               } else if (!(diff <= 0.0) && linkedCrystal.balanceMode() != 2) {
                  this.transferRatesArrays.get(this.linkedPosCache.indexOf(linkedPos))[this.tick % 20] = this.balanceTransfer(linkedCrystal, diff);
               } else {
                  this.transferRatesArrays.get(this.linkedPosCache.indexOf(linkedPos))[this.tick % 20] = 0;
               }
            }
         }
      }
   }

   protected int balanceTransfer(ICrystalLink sendTo, double capDiff) {
      long stored = this.getEnergyStored();
      if (stored <= 0L) {
         return 0;
      } else {
         double transferCap = (double)Math.min(this.getMaxEnergyStored(), sendTo.getMaxEnergyStored());
         int energyToEqual = (int)(capDiff * (double)sendTo.getMaxEnergyStored() / 2.1);
         double maxFlow = Math.min((double)energyToEqual, Math.min(transferCap, (double)(sendTo.getMaxEnergyStored() - sendTo.getEnergyStored())));
         double flowRate = Math.min(1.0, capDiff * 10.0);
         int transfer = (int)(flowRate * maxFlow);
         double minFlow = 0.002 * transferCap;
         if ((double)transfer < minFlow) {
            transfer = (int)Math.min(minFlow, (double)energyToEqual);
         }

         int var16;
         sendTo.modifyEnergyStored((long)(var16 = (int)this.opStorage.modifyEnergyStored((long)(-transfer))));
         return var16;
      }
   }

   public void rebuildTransferList() {
      this.transferRatesArrays.clear();
      this.flowRates.clear();

      for (int i = 0; i < this.linkedCrystals.size(); i++) {
         this.transferRatesArrays.add(new int[20]);
         this.flowRates.add((byte)0);
      }
   }

   @Nonnull
   @Override
   public List<BlockPos> getLinks() {
      if (this.linkedPosCache == null || this.linkedPosCache.size() != this.linkedCrystals.size()) {
         this.linkedPosCache = new LinkedList<>();

         for (Vec3B offset : this.linkedCrystals) {
            this.linkedPosCache.add(this.fromOffset(offset));
         }

         this.fxHandler.reloadConnections();
         this.updateBlock();
      }

      return this.linkedPosCache;
   }

   @Override
   public boolean binderUsed(Player player, BlockPos linkTarget, Direction sideClicked) {
      BlockEntity te = this.level.getBlockEntity(linkTarget);
      if (!(te instanceof ICrystalLink target)) {
         ChatHelper.sendDeDupeIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.device_invalid").withStyle(ChatFormatting.RED), MSG_ID);
         return false;
      } else if (this.getLinks().contains(te.getBlockPos())) {
         this.breakLink(te.getBlockPos());
         target.breakLink(this.worldPosition);
         ChatHelper.sendDeDupeIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.link_broken").withStyle(ChatFormatting.GREEN), MSG_ID);
         return true;
      } else if (this.getLinks().size() >= this.maxLinks()) {
         ChatHelper.sendDeDupeIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.link_limit_reached_this").withStyle(ChatFormatting.RED), MSG_ID
         );
         return false;
      } else if (target.getLinks().size() >= target.maxLinks()) {
         ChatHelper.sendDeDupeIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.link_limit_reached_target").withStyle(ChatFormatting.RED), MSG_ID
         );
         return false;
      } else if (!Utils.inRangeSphere(this.worldPosition, linkTarget, this.maxLinkRange())) {
         ChatHelper.sendDeDupeIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.this_range_limit").withStyle(ChatFormatting.RED), MSG_ID);
         return false;
      } else if (!Utils.inRangeSphere(this.worldPosition, linkTarget, target.maxLinkRange())) {
         ChatHelper.sendDeDupeIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.target_range_limit").withStyle(ChatFormatting.RED), MSG_ID
         );
         return false;
      } else if (!target.createLink(this)) {
         ChatHelper.sendDeDupeIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.link_failed_unknown").withStyle(ChatFormatting.RED), MSG_ID
         );
         return false;
      } else if (!this.createLink(target)) {
         target.breakLink(this.worldPosition);
         ChatHelper.sendDeDupeIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.link_failed_unknown").withStyle(ChatFormatting.RED), MSG_ID
         );
         return false;
      } else {
         ChatHelper.sendDeDupeIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.devices_linked").withStyle(ChatFormatting.GREEN), MSG_ID);
         return true;
      }
   }

   @Override
   public boolean createLink(ICrystalLink otherCrystal) {
      Vec3B offset = this.getOffset(((BlockEntity)otherCrystal).getBlockPos());
      this.linkedCrystals.add(offset);
      this.linkedPosCache = null;
      this.updateBlock();
      this.dirtyBlock();
      this.fxHandler.reloadConnections();
      return true;
   }

   @Override
   public void breakLink(BlockPos otherCrystal) {
      Vec3B offset = this.getOffset(otherCrystal);
      if (this.linkedCrystals.contains(offset)) {
         this.linkedCrystals.remove(offset);
      }

      this.linkedPosCache = null;
      this.updateBlock();
      this.dirtyBlock();
      this.fxHandler.reloadConnections();
   }

   @Override
   public int maxLinks() {
      return MAX_LINKS.get(this.getCrystalType())[this.getTier()];
   }

   @Override
   public int maxLinkRange() {
      switch (this.getTier()) {
         case 0:
            return 32;
         case 1:
            return 64;
         case 2:
            return 127;
         default:
            return 0;
      }
   }

   @Override
   public int balanceMode() {
      return 1;
   }

   @Override
   public long getEnergyStored() {
      return (long)this.opStorage.getEnergyStored();
   }

   @Override
   public long getMaxEnergyStored() {
      return (long)this.opStorage.getMaxEnergyStored();
   }

   @Override
   public void modifyEnergyStored(long energy) {
      this.opStorage.modifyEnergyStored(energy);
   }

   @Override
   public int getTier() {
      return this.techLevel.index;
   }

   public abstract EnergyCrystal.CrystalType getCrystalType();

   private int getCapacityForTier(int tier) {
      switch (tier) {
         case 0:
            return 4000000;
         case 1:
            return 16000000;
         case 2:
            return 64000000;
         default:
            return 0;
      }
   }

   public Vec3B getOffset(BlockPos target) {
      return new Vec3B(this.worldPosition.subtract(target));
   }

   public BlockPos fromOffset(Vec3B targetOffset) {
      return this.worldPosition.subtract(targetOffset.getPos());
   }

   public ENetFXHandler getFxHandler() {
      return this.fxHandler;
   }

   public byte calculateFlow(int index) {
      long sum = 0L;

      for (int transfer : this.transferRatesArrays.get(index)) {
         sum += (long)transfer;
      }

      double rf = (double)(sum / 20L);
      double d = rf / ((double)this.getMaxEnergyStored() * 0.001 + rf);
      return (byte)((int)(d * 255.0));
   }

   public void getLinkData(List<TileCrystalBase.LinkData> data) {
      for (BlockPos target : this.getLinks()) {
         BlockEntity tile = this.level.getBlockEntity(target);
         if (tile != null) {
            TileCrystalBase.LinkData ld = new TileCrystalBase.LinkData();
            ld.displayName = tile.getClass().getSimpleName();
            long sum = 0L;

            for (int transfer : this.transferRatesArrays.get(this.linkedPosCache.indexOf(target))) {
               sum += (long)transfer;
            }

            ld.transferPerTick = (int)(sum / 20L);
            ld.linkTarget = target;
            ld.data = "Data...";
            data.add(ld);
         }
      }
   }

   public String getUnlocalizedName() {
      return "tile.crystalnetwork:energy_crystal."
         + this.getCrystalType().getSerializedName()
         + "."
         + (this.getTier() == 0 ? "basic" : (this.getTier() == 1 ? "wyvern" : "draconic"))
         + ".name";
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public abstract CrystalFXBase createStaticFX();

   public void addDisplayData(List<Component> displayList) {
      double charge = MathUtils.round((double)this.getEnergyStored() / (double)this.getMaxEnergyStored() * 100.0, 100.0);
      displayList.add(
         Component.translatable("gui.crystalnetwork.energy_net.hud_charge")
            .append(": " + Utils.formatNumber(this.getEnergyStored()) + " / " + Utils.formatNumber(this.getMaxEnergyStored()) + " RF [" + charge + "%]")
            .withStyle(ChatFormatting.BLUE)
      );
      displayList.add(
         Component.translatable("gui.crystalnetwork.energy_net.hud_links")
            .append(": " + this.getLinks().size() + " / " + this.maxLinks())
            .withStyle(ChatFormatting.GREEN)
      );
   }

   @Override
   public ENetFXHandler createServerFXHandler() {
      return new ENetFXHandlerServer(this);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public ENetFXHandler createClientFXHandler() {
      return new ENetFXHandlerClient(this);
   }

   public void writeExtraNBT(Provider provider, CompoundTag compound) {
      compound.putByte("tech_level", (byte)this.techLevel.ordinal());
      ListTag list = new ListTag();

      for (Vec3B vec : this.linkedCrystals) {
         list.add(new ByteArrayTag(new byte[]{vec.x, vec.y, vec.z}));
      }

      compound.put("linked_crystals", list);
      this.fxHandler.writeToNBT(compound);
      byte[] array = new byte[this.flowRates.size()];

      for (int i = 0; i < array.length; i++) {
         array[i] = this.flowRates.get(i);
      }

      compound.putByteArray("flow_rates", array);
      super.writeExtraNBT(provider, compound);
   }

   public void readExtraNBT(Provider provider, CompoundTag compound) {
      this.techLevel = TechLevel.values()[compound.getInt("tech_level")];
      ListTag list = compound.getList("linked_crystals", 7);
      this.linkedCrystals.clear();

      for (int i = 0; i < list.size(); i++) {
         byte[] data = ((ByteArrayTag)list.get(i)).getAsByteArray();
         this.linkedCrystals.add(new Vec3B(data[0], data[1], data[2]));
      }

      if (this.linkedPosCache != null) {
         this.linkedPosCache.clear();
      }

      this.fxHandler.readFromNBT(compound);
      if (compound.contains("flow_rates")) {
         byte[] array = compound.getByteArray("flow_rates");
         this.flowRates.clear();

         for (byte b : array) {
            this.flowRates.add(b);
         }
      }

      int cap = this.getCapacityForTier(this.getTier());
      this.opStorage.setCapacity((long)cap).setMaxTransfer((long)cap);
      super.readExtraNBT(provider, compound);
   }

   public void onTilePlaced(BlockPlaceContext context, BlockState state) {
      int cap = this.getCapacityForTier(this.getTier());
      this.opStorage.setCapacity((long)cap).setMaxTransfer((long)cap);
   }

   @Override
   public int getIDHash() {
      if (!this.hashCached) {
         this.hashID = this.worldPosition.hashCode();
         this.hashCached = true;
      }

      return this.hashID;
   }

   public void onLoad() {
      super.onLoad();
      if (!CrystalUpdateBatcher.ID_CRYSTAL_MAP.containsKey(this.getIDHash())) {
         CrystalUpdateBatcher.ID_CRYSTAL_MAP.put(this.getIDHash(), this.worldPosition);
      }
   }

   public void onChunkUnloaded() {
      if (CrystalUpdateBatcher.ID_CRYSTAL_MAP.containsKey(this.getIDHash())) {
         CrystalUpdateBatcher.ID_CRYSTAL_MAP.remove(this.getIDHash());
      }

      this.fxHandler.tileUnload();
   }

   public void receiveBatchedUpdate(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
      this.fxHandler.updateReceived(update);
   }

   public void detectAndSendContainerChanges(List<ContainerListener> listeners) {
      if (this.linkedCrystals.size() != this.transferRatesArrays.size() && !this.level.isClientSide) {
         this.rebuildTransferList();
      }

      List<BlockPos> positions = this.getLinks();
      ListTag list = new ListTag();

      for (BlockPos lPos : positions) {
         int index = positions.indexOf(lPos);
         if (!this.containerEnergyFlow.containsKey(index) || this.containerEnergyFlow.get(index) != this.getLinkFlow(index)) {
            this.containerEnergyFlow.put(index, this.getLinkFlow(index));
            CompoundTag data = new CompoundTag();
            data.putByte("I", (byte)index);
            data.putInt("E", this.getLinkFlow(index));
            list.add(data);
         }
      }

      if (!list.isEmpty()) {
         CompoundTag compound = new CompoundTag();
         compound.put("L", list);
         this.sendUpdateToListeners(listeners, this.sendPacketToClient(output -> output.writeCompoundNBT(compound), 0));
      } else if (this.containerEnergyFlow.size() > this.linkedCrystals.size()) {
         this.containerEnergyFlow.clear();
         this.sendUpdateToListeners(listeners, this.sendPacketToClient(output -> {
         }, 1));
      }
   }

   public void sendUpdateToListeners(List<ContainerListener> listeners, PacketCustom packet) {
      for (ContainerListener listener : listeners) {
         if (listener instanceof ServerPlayer) {
            packet.sendToPlayer((ServerPlayer)listener);
         }
      }
   }

   public void receivePacketFromServer(MCDataInput data, int id) {
      if (id == 0) {
         CompoundTag compound = data.readCompoundNBT();
         ListTag list = compound.getList("L", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag tagData = list.getCompound(i);
            this.containerEnergyFlow.put(Integer.valueOf(tagData.getByte("I")), tagData.getInt("E"));
         }
      }
   }

   public void receivePacketFromClient(MCDataInput data, ServerPlayer client, int id) {
      if (id == 10) {
         int intValue = data.readInt();
         if (this.getLinks().size() > intValue && intValue >= 0) {
            BlockPos target = this.getLinks().get(intValue);
            this.breakLink(target);
            BlockEntity targetTile = this.level.getBlockEntity(target);
            if (targetTile instanceof ICrystalLink) {
               ((ICrystalLink)targetTile).breakLink(this.worldPosition);
            }
         }
      } else if (id == 20) {
         for (BlockPos target : new ArrayList<>(this.getLinks())) {
            this.breakLink(target);
            BlockEntity targetTile = this.level.getBlockEntity(target);
            if (targetTile instanceof ICrystalLink) {
               ((ICrystalLink)targetTile).breakLink(this.worldPosition);
            }
         }
      }
   }

   public int getLinkFlow(int linkIndex) {
      if (this.transferRatesArrays.size() <= linkIndex) {
         return 0;
      } else {
         long sum = 0L;

         for (int i : this.transferRatesArrays.get(linkIndex)) {
            sum += (long)i;
         }

         return (int)(sum / 20L);
      }
   }

   @Override
   public LinkedList<Byte> getFlowRates() {
      return this.flowRates;
   }

   static {
      MAX_LINKS.put(EnergyCrystal.CrystalType.RELAY, new int[]{8, 16, 32});
      MAX_LINKS.put(EnergyCrystal.CrystalType.CRYSTAL_IO, new int[]{2, 3, 4});
      MAX_LINKS.put(EnergyCrystal.CrystalType.WIRELESS, new int[]{4, 8, 16});
   }

   public static class LinkData {
      public String displayName;
      public int transferPerTick;
      public BlockPos linkTarget;
      public String data;

      public LinkData() {
      }

      public LinkData(String displayName, int transferPerTick, BlockPos linkTarget, String data) {
         this.displayName = displayName;
         this.transferPerTick = transferPerTick;
         this.linkTarget = linkTarget;
         this.data = data;
      }

      public void toBytes(ByteBuf buf) {
         buf.writeInt(this.transferPerTick);
         buf.writeLong(this.linkTarget.asLong());
      }

      public static TileCrystalBase.LinkData fromBytes(ByteBuf buf) {
         TileCrystalBase.LinkData data = new TileCrystalBase.LinkData();
         data.transferPerTick = buf.readInt();
         data.linkTarget = BlockPos.of(buf.readLong());
         return data;
      }

      @Override
      public int hashCode() {
         return super.hashCode();
      }
   }
}
