package dev.crystalnetwork.ported.network;

import codechicken.lib.packet.ICustomPacketHandler.IClientPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.Minecraft;

public final class CrystalNetworkClientHandler implements IClientPacketHandler {
    @Override
    public void handlePacket(PacketCustom packet, Minecraft minecraft) {
        if (packet.getType() == 1) CrystalUpdateBatcher.handleBatchedData(packet, minecraft.level);
    }
}
