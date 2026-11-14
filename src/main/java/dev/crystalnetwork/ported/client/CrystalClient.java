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
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class CrystalClient {
    public static void register(IEventBus modBus) {
        ClientEventHandler.register();
        AtlasTextureHelper.register(modBus);
        CrystalShaders.register(modBus);
        modBus.addListener(CrystalClient::registerRenderers);
        modBus.addListener(CrystalClient::registerBlockColours);
        NeoForge.EVENT_BUS.addListener(CrystalClient::renderBinderOverlay);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CrystalContent.TILE_RELAY_CRYSTAL.get(), RenderTileEnergyCrystal::new);
        event.registerBlockEntityRenderer(CrystalContent.TILE_IO_CRYSTAL.get(), RenderTileEnergyCrystal::new);
        event.registerBlockEntityRenderer(CrystalContent.TILE_WIRELESS_CRYSTAL.get(), RenderTileEnergyCrystal::new);
    }

    private static void registerBlockColours(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> particleColour(0),
                CrystalContent.BASIC_RELAY.get(), CrystalContent.BASIC_IO.get(), CrystalContent.BASIC_WIRELESS.get());
        event.register((state, level, pos, tint) -> particleColour(1),
                CrystalContent.WYVERN_RELAY.get(), CrystalContent.WYVERN_IO.get(), CrystalContent.WYVERN_WIRELESS.get());
        event.register((state, level, pos, tint) -> particleColour(2),
                CrystalContent.DRACONIC_RELAY.get(), CrystalContent.DRACONIC_IO.get(), CrystalContent.DRACONIC_WIRELESS.get());
    }

    private static int particleColour(int tier) {
        float[] colour = RenderTileEnergyCrystal.COLOURS[tier];
        return (Math.round(colour[0] * 255.0F) << 16)
                | (Math.round(colour[1] * 255.0F) << 8)
                | Math.round(colour[2] * 255.0F);
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
