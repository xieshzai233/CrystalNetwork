package dev.crystalnetwork.ported.network;

import codechicken.lib.packet.PacketCustomChannel;
import dev.crystalnetwork.CrystalNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public final class CrystalNetworkPackets {
    public static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(CrystalNetwork.MOD_ID, "network");
    private static final PacketCustomChannel CHANNEL = new PacketCustomChannel(CHANNEL_NAME)
            .versioned("1.0.0")
            .client(() -> CrystalNetworkClientHandler::new)
            .server(() -> CrystalNetworkServerHandler::new);

    public static void register(IEventBus bus) { CHANNEL.init(bus); }
    private CrystalNetworkPackets() { }
}
