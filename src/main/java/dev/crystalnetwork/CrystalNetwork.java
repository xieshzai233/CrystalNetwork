package dev.crystalnetwork;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import dev.crystalnetwork.ported.init.CrystalContent;
import dev.crystalnetwork.ported.init.ItemData;
import dev.crystalnetwork.ported.handlers.CrystalNetworkEvents;
import dev.crystalnetwork.ported.network.CrystalUpdateBatcher;
import dev.crystalnetwork.ported.network.CrystalNetworkPackets;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.api.distmarker.Dist;
import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.client.CrystalClient;

@Mod(CrystalNetwork.MOD_ID)
public final class CrystalNetwork {
    public static final String MOD_ID = "crystalnetwork";
    public static long serverTicks;

    public CrystalNetwork(IEventBus modBus) {
        CrystalContent.register(modBus);
        ItemData.register(modBus);
        CrystalNetworkPackets.register(modBus);
        modBus.addListener(CrystalNetworkEvents::registerCapabilities);
        Utils.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CrystalClient.register(modBus));
        NeoForge.EVENT_BUS.addListener(CrystalNetworkEvents::rightClickBlock);
        NeoForge.EVENT_BUS.addListener(CrystalNetworkEvents::rightClickItem);
        NeoForge.EVENT_BUS.addListener(CrystalNetwork::serverTick);
    }

    private static void serverTick(ServerTickEvent.Post event) {
        serverTicks++;
        CrystalUpdateBatcher.tickEnd();
    }
}
