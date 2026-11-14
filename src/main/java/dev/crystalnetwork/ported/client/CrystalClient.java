package dev.crystalnetwork.ported.client;

import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import dev.crystalnetwork.ported.client.render.tile.RenderTileEnergyCrystal;
import dev.crystalnetwork.ported.init.CrystalContent;
import dev.crystalnetwork.ported.handlers.BinderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class CrystalClient {
    public static void register(IEventBus modBus) {
        ClientEventHandler.register(modBus);
        AtlasTextureHelper.register(modBus);
        CrystalShaders.register(modBus);
        modBus.addListener(CrystalClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(CrystalClient::renderBinderOverlay);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CrystalContent.TILE_RELAY_CRYSTAL.get(), RenderTileEnergyCrystal::new);
        event.registerBlockEntityRenderer(CrystalContent.TILE_IO_CRYSTAL.get(), RenderTileEnergyCrystal::new);
        event.registerBlockEntityRenderer(CrystalContent.TILE_WIRELESS_CRYSTAL.get(), RenderTileEnergyCrystal::new);
    }

    private static void renderBinderOverlay(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof dev.crystalnetwork.ported.items.CrystalBinder)) {
            stack = player.getOffhandItem();
        }
        if (stack.getItem() instanceof dev.crystalnetwork.ported.items.CrystalBinder) {
            Level level = player.getCommandSenderWorld();
            BinderHandler.renderWorldOverlay(player, event.getPoseStack(), level, stack, Minecraft.getInstance(),
                    event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    private CrystalClient() { }
}
