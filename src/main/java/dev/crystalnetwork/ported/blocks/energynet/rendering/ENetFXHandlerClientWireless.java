package dev.crystalnetwork.ported.blocks.energynet.rendering;

import com.brandon3055.brandonscore.lib.Vec3D;
import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.client.DEParticles;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBase;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXBeam;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXLink;
import dev.crystalnetwork.ported.client.render.effect.CrystalFXWireless;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.LinkedList;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ENetFXHandlerClientWireless extends ENetFXHandler<TileCrystalWirelessIO> {
   protected CrystalFXBase staticFX;
   protected LinkedList<CrystalFXBase> beamFXList = new LinkedList<>();
   protected LinkedList<CrystalFXBase> transferFXList = new LinkedList<>();
   protected LinkedList<CrystalFXBase> linkFX = null;

   public ENetFXHandlerClientWireless(TileCrystalWirelessIO tile) {
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

         int i = this.beamFXList.indexOf(beam);
         if (this.tile.flowRates.size() > i && i >= 0) {
            beam.updateFX((float)(this.tile.flowRates.get((byte)i) & 255) / 255.0F);
         }
      }

      for (CrystalFXBase transFX : this.transferFXList) {
         if (!transFX.isAlive()) {
            requiresUpdate = true;
         }

         int i = this.transferFXList.indexOf(transFX);
         if (this.tile.receiverFlowRates.size() > i && i >= 0) {
            transFX.updateFX((float)(this.tile.receiverFlowRates.get(i) & 255) / 255.0F);
         }
      }

      if (requiresUpdate || this.tile.getReceivers().size() != this.transferFXList.size() || this.tile.getLinks().size() != this.beamFXList.size()) {
         this.reloadConnections();
      }

      if (ClientEventHandler.playerHoldingWrench) {
         if (this.linkFX == null || this.linkFX.size() != this.tile.getReceivers().size()) {
            if (this.linkFX != null) {
               for (CrystalFXBase fx : this.linkFX) {
                  fx.remove();
               }
            }

            this.linkFX = new LinkedList<>();

            for (BlockPos receiver : this.tile.getReceivers()) {
               CrystalFXLink link = new CrystalFXLink((ClientLevel)this.tile.getLevel(), this.tile, Vec3D.getCenter(receiver));
               this.linkFX.add(link);
               DEParticles.addParticleDirect(this.tile.getLevel(), link);
            }
         }
      } else if (this.linkFX != null) {
         this.linkFX = null;
      }
   }

   @Override
   public void updateReceived(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
      this.tile.modifyEnergyStored(update.crystalCapacity - this.tile.getEnergyStored());
      Map<Byte, Byte> flowMap = update.indexToFlowMap;

      for (byte index = 0; index < this.tile.flowRates.size(); index++) {
         if (!flowMap.containsKey(index)) {
            flowMap.put(index, this.tile.flowRates.get(index));
         }
      }

      for (byte indexx = 0; indexx < this.tile.receiverFlowRates.size(); indexx++) {
         if (!flowMap.containsKey((byte)(indexx + 128))) {
            flowMap.put((byte)(indexx + 128), this.tile.receiverFlowRates.get(indexx));
         }
      }

      this.tile.flowRates.clear();
      this.tile.receiverFlowRates.clear();

      for (byte b : flowMap.keySet()) {
         if ((b & 255) >= 128) {
            this.tile.receiverFlowRates.add(flowMap.get(b));
         } else {
            this.tile.flowRates.add(flowMap.get(b));
         }
      }
   }

   @Override
   public void reloadConnections() {
      this.beamFXList.clear();
      this.transferFXList.clear();

      for (BlockPos pos : this.tile.getLinks()) {
         BlockEntity target = this.tile.getLevel().getBlockEntity(pos);
         if (target instanceof ICrystalLink) {
            CrystalFXBeam beam = new CrystalFXBeam(this.tile.getLevel(), this.tile, (ICrystalLink)target);
            this.beamFXList.add(beam);
            DEParticles.addParticleDirect(this.tile.getLevel(), beam);
         }
      }

      for (BlockPos posx : this.tile.getReceivers()) {
         CrystalFXWireless wirelessFX = new CrystalFXWireless((ClientLevel)this.tile.getLevel(), this.tile, posx);
         this.transferFXList.add(wirelessFX);
         DEParticles.addParticleDirect(this.tile.getLevel(), wirelessFX);
      }
   }

   @Override
   public void tileUnload() {
      super.tileUnload();
   }
}
