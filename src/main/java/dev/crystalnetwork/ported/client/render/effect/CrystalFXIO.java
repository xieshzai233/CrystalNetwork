package dev.crystalnetwork.ported.client.render.effect;

import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CrystalFXIO extends CrystalFXBase<TileCrystalBase> {
   private static final ParticleRenderType BASIC_HANDLER = new CrystalFXIO.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_basic.png")
   );
   private static final ParticleRenderType WYVERN_HANDLER = new CrystalFXIO.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_wyvern.png")
   );
   private static final ParticleRenderType DRACONIC_HANDLER = new CrystalFXIO.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_draconic.png")
   );

   public CrystalFXIO(ClientLevel worldIn, TileCrystalBase tile) {
      super(worldIn, tile);
      this.age = worldIn.random.nextInt(1024);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.ticksTillDeath-- <= 0) {
         this.remove();
      }

      float[] r = new float[]{0.0F, 0.8F, 1.0F};
      float[] g = new float[]{0.8F, 0.1F, 0.7F};
      float[] b = new float[]{1.0F, 1.0F, 0.2F};
      this.rCol = r[this.tile.getTier()];
      this.gCol = g[this.tile.getTier()];
      this.bCol = b[this.tile.getTier()];
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      if (this.renderEnabled) {
         Vec3 viewVec = renderInfo.getPosition();
         float viewX = (float)(this.x - viewVec.x());
         float viewY = (float)(this.y - viewVec.y());
         float viewZ = (float)(this.z - viewVec.z());
         Vector3f[] renderVector = this.getRenderVectors(renderInfo, viewX, viewY, viewZ, 0.2F);
         buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.5F, 0.5F).setUv2(240, 240);
         buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.5F, 0.0F).setUv2(240, 240);
         buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F).setUv2(240, 240);
         buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(0.0F, 0.5F).setUv2(240, 240);
      }
   }

   public ParticleRenderType getRenderType() {
      return this.tile.getTier() == 0 ? BASIC_HANDLER : (this.tile.getTier() == 1 ? WYVERN_HANDLER : DRACONIC_HANDLER);
   }

   public static class FXHandler implements ParticleRenderType {
      private final ResourceLocation texture;

      public FXHandler(ResourceLocation texture) {
         this.texture = texture;
      }

      @Nullable
      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
         RenderSystem.setShaderTexture(0, this.texture);
         return tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
      }
   }
}
