package dev.crystalnetwork.ported.network;

import codechicken.lib.packet.ICustomPacketHandler.IServerPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.server.level.ServerPlayer;

public final class CrystalNetworkServerHandler implements IServerPacketHandler {
    @Override
    public void handlePacket(PacketCustom packet, ServerPlayer player) { }
}
