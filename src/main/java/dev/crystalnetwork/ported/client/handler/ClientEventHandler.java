package dev.crystalnetwork.ported.client.handler;

import dev.crystalnetwork.ported.items.CrystalBinder;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientEventHandler {
    public static int elapsedTicks;
    public static boolean playerHoldingWrench;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientEventHandler::clientTick);
    }

    private static void clientTick(ClientTickEvent.Post event) {
        elapsedTicks++;
        var player = Minecraft.getInstance().player;
        playerHoldingWrench = player != null && (player.getMainHandItem().getItem() instanceof CrystalBinder
                || player.getOffhandItem().getItem() instanceof CrystalBinder);
    }

    private ClientEventHandler() { }
}
