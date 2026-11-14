package dev.crystalnetwork.ported.network;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;

public class CrystalUpdateBatcher {
   public static final Map<Integer, BlockPos> ID_CRYSTAL_MAP = new HashMap<>();
   private static final Map<ServerPlayer, List<CrystalUpdateBatcher.BatchedCrystalUpdate>> batchQue = new HashMap<>();

   public static void queData(CrystalUpdateBatcher.BatchedCrystalUpdate update, ServerPlayer target) {
      batchQue.computeIfAbsent(target, e -> new ArrayList<>()).add(update);
   }

   public static void tickEnd() {
      if (!batchQue.isEmpty()) {
         for (ServerPlayer playerMP : batchQue.keySet()) {
            List<CrystalUpdateBatcher.BatchedCrystalUpdate> playerData = batchQue.get(playerMP);
            if (!playerData.isEmpty()) {
               PacketCustom packet = new PacketCustom(CrystalNetworkPackets.CHANNEL_NAME, 1, playerMP.registryAccess());
               packet.writeVarInt(playerData.size());
               playerData.forEach(update -> update.writeData(packet));
               packet.sendToPlayer(playerMP);
            }
         }

         batchQue.clear();
      }
   }

   public static void handleBatchedData(MCDataInput input, Level clientWorld) {
      int count = input.readVarInt();

      for (int i = 0; i < count; i++) {
         CrystalUpdateBatcher.BatchedCrystalUpdate update = new CrystalUpdateBatcher.BatchedCrystalUpdate();
         update.readData(input);
         if (ID_CRYSTAL_MAP.containsKey(update.crystalID)) {
            BlockEntity tile = clientWorld == null ? null : clientWorld.getBlockEntity(ID_CRYSTAL_MAP.get(update.crystalID));
            if (tile instanceof TileCrystalBase && !tile.isRemoved()) {
               ((TileCrystalBase)tile).receiveBatchedUpdate(update);
            }
         }
      }
   }

   public static class BatchedCrystalUpdate {
      public int crystalID;
      public long crystalCapacity;
      public Map<Byte, Byte> indexToFlowMap = new LinkedHashMap<>();

      public BatchedCrystalUpdate() {
      }

      public BatchedCrystalUpdate(int crystalID, long crystalCapacity) {
         this.crystalID = crystalID;
         this.crystalCapacity = crystalCapacity;
      }

      public void writeData(MCDataOutput output) {
         output.writeVarInt(this.crystalID);
         output.writeVarLong(this.crystalCapacity);
         output.writeByte(this.indexToFlowMap.size());

         for (Byte index : this.indexToFlowMap.keySet()) {
            output.writeByte(index);
            output.writeByte(this.indexToFlowMap.get(index));
         }
      }

      public void readData(MCDataInput input) {
         this.crystalID = input.readVarInt();
         this.crystalCapacity = input.readVarLong();
         byte count = input.readByte();

         for (int i = 0; i < count; i++) {
            byte index = input.readByte();
            this.indexToFlowMap.put(index, input.readByte());
         }
      }
   }
}
