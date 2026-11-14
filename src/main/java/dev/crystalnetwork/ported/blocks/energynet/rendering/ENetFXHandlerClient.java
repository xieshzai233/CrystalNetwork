package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.client.DEParticles;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBeam;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.LinkedList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ENetFXHandlerClient<T extends BlockEntity & IENetEffectTile> extends ENetFXHandler<T> {
   protected CrystalFXBase staticFX;
   protected LinkedList<CrystalFXBase> beamFXList = new LinkedList<>();

   public ENetFXHandlerClient(T tile) {
      super(tile);
   }

   @Override
   public void update() {
      if (this.tile.hasStaticFX()) {
         if (this.staticFX == null || !this.staticFX.isAlive()) {
            this.staticFX = this.tile.createStaticFX();
            DEParticles.addParticleDirect(this.tile.getLevel(), this.staticFX);
         }

         this.staticFX.updateFX(0.5F);
      }

      boolean requiresUpdate = false;

      for (CrystalFXBase beam : this.beamFXList) {
         if (!beam.isAlive()) {
            requiresUpdate = true;
         }

         if (this.tile.getFlowRates().size() > this.beamFXList.indexOf(beam)) {
            beam.updateFX((float)(this.tile.getFlowRates().get((byte)this.beamFXList.indexOf(beam)) & 255) / 255.0F);
         }
      }

      if (requiresUpdate || this.tile.getLinks().size() != this.beamFXList.size()) {
         this.reloadConnections();
      }
   }

   @Override
   public void updateReceived(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
      this.tile.modifyEnergyStored(update.crystalCapacity - this.tile.getEnergyStored());
      Map<Byte, Byte> flowMap = update.indexToFlowMap;

      for (byte index = 0; index < this.tile.getFlowRates().size(); index++) {
         if (!flowMap.containsKey(index)) {
            flowMap.put(index, this.tile.getFlowRates().get(index));
         }
      }

      this.tile.getFlowRates().clear();

      for (byte i = 0; i < flowMap.size(); i++) {
         if (flowMap.containsKey(i)) {
            this.tile.getFlowRates().add(flowMap.get(i));
         }
      }
   }

   @Override
   public void reloadConnections() {
      this.beamFXList.clear();

      for (BlockPos pos : this.tile.getLinks()) {
         BlockEntity target = this.tile.getLevel().getBlockEntity(pos);
         if (target instanceof ICrystalLink) {
            CrystalFXBeam beam = new CrystalFXBeam(this.tile.getLevel(), this.tile, (ICrystalLink)target);
            this.beamFXList.add(beam);
            DEParticles.addParticleDirect(this.tile.getLevel(), beam);
         }
      }
   }

   @Override
   public void tileUnload() {
      super.tileUnload();
   }
}
