package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@Deprecated
public abstract class ENetFXHandler<T extends BlockEntity & IENetEffectTile> {
   protected final T tile;

   public ENetFXHandler(T tile) {
      this.tile = tile;
   }

   public abstract void update();

   public void detectAndSendChanges() {
   }

   public void tileUnload() {
   }

   public void updateReceived(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
   }

   public abstract void reloadConnections();

   public void writeToNBT(CompoundTag compound) {
   }

   public void readFromNBT(CompoundTag compound) {
   }
}
