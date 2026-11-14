package dev.crystalnetwork.ported.client;

import codechicken.lib.render.shader.CCShaderInstance;
import codechicken.lib.render.shader.CCUniform;
import com.brandon3055.brandonscore.api.TimeKeeper;
import com.brandon3055.brandonscore.client.render.RenderUtils;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.crystalnetwork.CrystalNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public final class CrystalShaders {
    public static CCShaderInstance energyCrystalShader;
    public static CCUniform energyCrystalTime;
    public static CCUniform energyCrystalColour;
    public static CCUniform energyCrystalMipmap;

    public static void register(IEventBus bus) { bus.addListener(CrystalShaders::registerShaders); }

    private static void registerShaders(RegisterShadersEvent event) {
        event.registerShader(CCShaderInstance.create(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(CrystalNetwork.MOD_ID, "energy_crystal"), DefaultVertexFormat.POSITION_TEX), shader -> {
            energyCrystalShader = (CCShaderInstance) shader;
            energyCrystalTime = energyCrystalShader.getUniform("Time");
            energyCrystalColour = energyCrystalShader.getUniform("Colour");
            energyCrystalMipmap = energyCrystalShader.getUniform("Mipmap");
            energyCrystalShader.onApply(() -> energyCrystalTime.glUniform1f(
                    ((float) TimeKeeper.getClientTick() + RenderUtils.partialTick()) / 50.0F));
        });
    }

    private CrystalShaders() { }
}
