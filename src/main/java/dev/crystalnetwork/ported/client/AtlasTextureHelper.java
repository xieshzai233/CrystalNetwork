package dev.crystalnetwork.ported.client;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

public final class AtlasTextureHelper {
    public static final TextureAtlasSprite[] ENERGY_PARTICLE = new TextureAtlasSprite[5];
    public static TextureAtlasSprite ORB_PARTICLE;

    public static void register(IEventBus bus) { bus.addListener(AtlasTextureHelper::stitched); }

    private static void stitched(TextureAtlasStitchedEvent event) {
        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) return;
        for (int i = 0; i < ENERGY_PARTICLE.length; i++) {
            ENERGY_PARTICLE[i] = event.getAtlas().getSprite(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "energy_" + i));
        }
        ORB_PARTICLE = event.getAtlas().getSprite(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "white_orb"));
    }

    private AtlasTextureHelper() { }
}
