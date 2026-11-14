package dev.crystalnetwork.ported.blocks.energynet.tileentity;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.lib.datamanager.DataFlags;
import com.brandon3055.brandonscore.lib.datamanager.ManagedBool;
import com.brandon3055.brandonscore.lib.datamanager.ManagedEnum;
import com.brandon3055.brandonscore.utils.EnergyUtils;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXIO;
import dev.crystalnetwork.ported.init.CrystalContent;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class TileCrystalDirectIO extends TileCrystalBase {
   public final ManagedEnum<Direction> facing = (ManagedEnum<Direction>)this.dataManager
      .register(new ManagedEnum("facing", Direction.DOWN, new DataFlags[]{DataFlags.SAVE_NBT_SYNC_TILE}));
   public final ManagedBool outputMode = (ManagedBool)this.dataManager.register(new ManagedBool("outputMode", new DataFlags[]{DataFlags.SAVE_BOTH_SYNC_TILE}));

   public TileCrystalDirectIO(BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_IO_CRYSTAL.get(), pos, state);
      this.outputMode.addValueListener(newVal -> this.opStorage.setIOMode(!newVal));
   }

   public TileCrystalDirectIO(TechLevel techLevel, BlockPos pos, BlockState state) {
      super(CrystalContent.TILE_IO_CRYSTAL.get(), techLevel, pos, state);
   }

   public static void register(RegisterCapabilitiesEvent event) {
      energyCapability(event, CrystalContent.TILE_IO_CRYSTAL);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         BlockEntity tile = this.level.getBlockEntity(this.worldPosition.relative((Direction)this.facing.get()));
         if (this.outputMode.get() && tile != null) {
            this.opStorage
               .extractOP(
                  EnergyUtils.insertEnergy(
                     tile, this.opStorage.extractOP(this.opStorage.maxExtract(), true), ((Direction)this.facing.get()).getOpposite(), false
                  ),
                  false
               );
         }
      }
   }

   public InteractionResult useWithoutItem(BlockState state, Player player, BlockHitResult hit) {
      if (player instanceof ServerPlayer && player.isShiftKeyDown()) {
         this.outputMode.invert();
         this.updateRotation((Direction)this.facing.get());
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public EnergyCrystal.CrystalType getCrystalType() {
      return EnergyCrystal.CrystalType.CRYSTAL_IO;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public CrystalFXBase createStaticFX() {
      return new CrystalFXIO((ClientLevel)this.level, this);
   }

   @Override
   public Vec3D getBeamLinkPos(BlockPos linkTo) {
      return Vec3D.getCenter(this.worldPosition);
   }

   @Override
   public boolean renderBeamTermination() {
      return false;
   }

   @Override
   public void addDisplayData(List<Component> displayList) {
      super.addDisplayData(displayList);
      ChatFormatting colour = this.outputMode.get() ? ChatFormatting.GOLD : ChatFormatting.DARK_AQUA;
      displayList.add(
         Component.translatable("gui.crystalnetwork.energy_net.io_mode")
            .append(" ")
            .append(Component.translatable("gui.crystalnetwork.energy_net.io_output_" + this.outputMode.get()).withStyle(colour))
      );
   }

   @Override
   public void onTilePlaced(BlockPlaceContext context, BlockState state) {
      super.onTilePlaced(context, state);
      this.updateRotation(context.getClickedFace().getOpposite());
   }

   public void updateRotation(Direction newDirection) {
      this.facing.set(newDirection);
      this.capManager.remove(CapabilityOP.BLOCK, new Direction[0]);
      this.opStorage.setIOMode(!this.outputMode.get());
      this.capManager.setSide(CapabilityOP.BLOCK, this.opStorage, newDirection);
   }

   @Override
   public void readExtraNBT(Provider provider, CompoundTag compound) {
      super.readExtraNBT(provider, compound);
      this.updateRotation((Direction)this.facing.get());
   }

   public void readFromItemStack(Provider provider, CompoundTag compound) {
      super.readFromItemStack(provider, compound);
      this.updateRotation((Direction)this.facing.get());
   }
}
