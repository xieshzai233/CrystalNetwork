package dev.crystalnetwork.ported.client.render.effect;

import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import dev.crystalnetwork.ported.client.AtlasTextureHelper;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CrystalFXRing extends CrystalFXBase<TileCrystalBase> {
   private long rSeed = 0L;
   public static final ParticleRenderType RENDER_TYPE = (tesselator, textureManager) -> {
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.setShader(GameRenderer::getParticleShader);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
      return tesselator.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
   };

   public CrystalFXRing(ClientLevel worldIn, TileCrystalBase tile) {
      super(worldIn, tile);
      this.age = worldIn.random.nextInt(1024);
      this.rSeed = tile.getBlockPos().asLong();
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

   public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
      if (this.renderEnabled && AtlasTextureHelper.ENERGY_PARTICLE != null && AtlasTextureHelper.ENERGY_PARTICLE[0] != null) {
         boolean wierless = this.tile.getCrystalType() == EnergyCrystal.CrystalType.WIRELESS;
         this.random.setSeed(this.rSeed);
         float animTime = (float)(ClientEventHandler.elapsedTicks + this.age) + partialTicks;
         Vec3 view = camera.getPosition();
         float viewX = (float)(this.x - view.x());
         float viewY = (float)(this.y - view.y());
         float viewZ = (float)(this.z - view.z());
         double mipLevel = Math.max(0.0, Math.min(1.0, (camera.getBlockPosition().distToCenterSqr(this.x, this.y, this.z) - 20.0) / 600.0));
         double pCount = 20.0 + 80.0 * (1.0 - mipLevel);

         for (int i = 0; (double)i < pCount; i++) {
            double rotation = (double)i / pCount * 6.282 + (double)animTime / 80.0;
            float rFloat3 = this.random.nextFloat();
            float rFloat4 = this.random.nextFloat();
            float scale = 0.01F + rFloat4 * 0.05F + (float)mipLevel * 0.2F;
            float a = 1.0F;
            float r = this.rCol;
            float g = this.gCol;
            float b = this.bCol;
            rotation -= 0.05F;
            double subRotationRadius = 0.1 * (double)rFloat3 + 0.02;
            double dir = this.random.nextBoolean() ? 1.0 : -1.0;
            double sy = Math.cos(dir * rotation * (double)(rFloat3 * 10.0F) * (double)(1.0F - this.random.nextFloat() * 0.2F)) * subRotationRadius;
            double sx = Math.sin(dir * rotation * (double)(rFloat3 * 10.0F) * (double)(1.0F - this.random.nextFloat() * 0.2F)) * subRotationRadius;
            float drawY = viewY + (float)sy;
            double renderRadius = 0.4 + sx;
            double ox = Math.sin(rotation) * renderRadius;
            double oz = Math.cos(rotation) * renderRadius;
            float drawX = viewX + (float)ox;
            float drawZ = viewZ + (float)oz;
            int texIndex = ClientEventHandler.elapsedTicks % AtlasTextureHelper.ENERGY_PARTICLE.length;
            TextureAtlasSprite sprite = AtlasTextureHelper.ENERGY_PARTICLE[texIndex];
            float minU = sprite.getU0();
            float maxU = sprite.getU1();
            float minV = sprite.getV0();
            float maxV = sprite.getV1();
            Vector3f[] renderVector = this.getRenderVectors(camera, drawX, drawY, drawZ, scale);
            buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(r, g, b, a).setUv(maxU, maxV).setUv2(240, 240);
            buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(r, g, b, a).setUv(maxU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(r, g, b, a).setUv(minU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(r, g, b, a).setUv(minU, maxV).setUv2(240, 240);
            scale = 0.01F + rFloat4 * 0.04F * (float)Math.sin((double)((animTime + (float)i) / 30.0F)) + (float)mipLevel * 0.05F;
            rotation = (double)i / pCount * 6.282 + (double)animTime / 200.0;
            rotation -= 0.05F;
            renderRadius = 0.4;
            ox = Math.sin(rotation) * renderRadius;
            oz = Math.cos(rotation) * renderRadius;
            drawX = viewX + (float)ox;
            drawZ = viewZ + (float)oz;
            r = wierless ? 1.0F : 0.0F;
            g = wierless ? 0.0F : 1.0F;
            b = wierless ? 0.0F : 1.0F;
            minU = AtlasTextureHelper.ORB_PARTICLE.getU0();
            maxU = AtlasTextureHelper.ORB_PARTICLE.getU1();
            minV = AtlasTextureHelper.ORB_PARTICLE.getV0();
            maxV = AtlasTextureHelper.ORB_PARTICLE.getV1();
            renderVector = this.getRenderVectors(camera, drawX, viewY, drawZ, scale);
            buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(r, g, b, a).setUv(maxU, maxV).setUv2(240, 240);
            buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(r, g, b, a).setUv(maxU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(r, g, b, a).setUv(minU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(r, g, b, a).setUv(minU, maxV).setUv2(240, 240);
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return RENDER_TYPE;
   }
}
