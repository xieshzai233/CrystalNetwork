package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class ENetFXHandlerServerWireless extends ENetFXHandler<TileCrystalWirelessIO> {
   private Map<Byte, Byte> lastTickIndexToFlow = new HashMap<>();
   private Map<Byte, Byte> lastTickIndexToRFlow = new HashMap<>();
   private long lastTickEnergy = -1L;

   public ENetFXHandlerServerWireless(TileCrystalWirelessIO tile) {
      super(tile);
   }

   @Override
   public void update() {
   }

   @Override
   public void detectAndSendChanges() {
      CrystalUpdateBatcher.BatchedCrystalUpdate update = new CrystalUpdateBatcher.BatchedCrystalUpdate(this.tile.getIDHash(), this.tile.getEnergyStored());

      for (byte i = 0; i < this.tile.flowRates.size(); i++) {
         byte flow = this.tile.flowRates.get(i);
         if (!this.lastTickIndexToFlow.containsKey(i) || this.lastTickIndexToFlow.get(i) != flow) {
            update.indexToFlowMap.put(i, flow);
            this.lastTickIndexToFlow.put(i, flow);
         }
      }

      for (byte ix = 0; ix < this.tile.receiverFlowRates.size(); ix++) {
         byte flow = this.tile.receiverFlowRates.get(ix);
         if (!this.lastTickIndexToRFlow.containsKey(ix) || this.lastTickIndexToRFlow.get(ix) != flow) {
            update.indexToFlowMap.put((byte)(ix + 128), flow);
            this.lastTickIndexToRFlow.put((byte)(ix + 128), flow);
         }
      }

      if (update.indexToFlowMap.size() > 0 || Math.abs(this.lastTickEnergy - this.tile.getEnergyStored()) > 100L) {
         this.lastTickEnergy = this.tile.getEnergyStored();
         this.queUpdate(update);
      }
   }

   @Override
   public void reloadConnections() {
      this.lastTickIndexToFlow.clear();
   }

   private void queUpdate(CrystalUpdateBatcher.BatchedCrystalUpdate update) {
      ServerLevel serverWorld = (ServerLevel)this.tile.getLevel();
      if (serverWorld != null) {
         serverWorld.getChunkSource()
            .chunkMap
            .getPlayers(new ChunkPos(this.tile.getBlockPos()), false)
            .forEach(player -> CrystalUpdateBatcher.queData(update, player));
      }
   }
}
