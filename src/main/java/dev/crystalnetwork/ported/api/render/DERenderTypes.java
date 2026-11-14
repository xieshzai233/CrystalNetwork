package dev.crystalnetwork.ported.api.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.LineStateShard;
import java.util.OptionalDouble;

public final class DERenderTypes {
    private static final DepthTestStateShard NO_DEPTH = new DepthTestStateShard("none", 519) {
        @Override public void setupRenderState() { RenderSystem.disableDepthTest(); }
    };
    public static final RenderType BOX_NO_DEPTH = RenderType.create("crystalnetwork:box_no_depth", DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 256, false, true,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(NO_DEPTH).createCompositeState(false));
    public static final RenderType OUTLINE_TYPE = RenderType.create("crystalnetwork:outline", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, 256,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE).setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setLineState(new LineStateShard(OptionalDouble.of(4.0))).createCompositeState(false));
    private DERenderTypes() { }
}
