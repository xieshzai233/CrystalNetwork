package dev.crystalnetwork.ported.blocks.energynet.rendering;

import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ENetFXHandlerServer<T extends BlockEntity & IENetEffectTile> extends ENetFXHandler<T> {
   private Map<Byte, Byte> lastTickIndexToFlow = new HashMap<>();
   private long lastTickEnergy = -1L;

   public ENetFXHandlerServer(T tile) {
      super(tile);
   }

   @Override
   public void update() {
   }

   @Override
   public void detectAndSendChanges() {
      this.lastTickIndexToFlow.clear();
      CrystalUpdateBatcher.BatchedCrystalUpdate update = new CrystalUpdateBatcher.BatchedCrystalUpdate(this.tile.getIDHash(), this.tile.getEnergyStored());

      for (byte i = 0; i < this.tile.getFlowRates().size(); i++) {
         byte flow = this.tile.getFlowRates().get(i);
         if (!this.lastTickIndexToFlow.containsKey(i) || this.lastTickIndexToFlow.get(i) != flow) {
            update.indexToFlowMap.put(i, flow);
            this.lastTickIndexToFlow.put(i, flow);
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
