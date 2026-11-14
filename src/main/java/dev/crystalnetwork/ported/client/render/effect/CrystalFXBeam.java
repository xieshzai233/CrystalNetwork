package dev.crystalnetwork.ported.client.render.effect;

import codechicken.lib.math.MathHelper;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.BCProfiler;
import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.api.energy.IENetEffectTile;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CrystalFXBeam<T extends BlockEntity & IENetEffectTile> extends CrystalFXBase<T> {
   private final Vec3D linkTarget;
   private final boolean terminateSource;
   private final boolean terminateTarget;
   private long lSeed = 0L;
   private boolean bolt = false;
   private float powerLevel = 0.0F;
   private static final ParticleRenderType BASIC_HANDLER = new CrystalFXBeam.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_basic.png")
   );
   private static final ParticleRenderType WYVERN_HANDLER = new CrystalFXBeam.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_wyvern.png")
   );
   private static final ParticleRenderType DRACONIC_HANDLER = new CrystalFXBeam.FXHandler(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/particle/energy_beam_draconic.png")
   );

   public CrystalFXBeam(Level worldIn, T tile, ICrystalLink linkTarget) {
      super((ClientLevel)worldIn, tile);
      this.age = worldIn.random.nextInt(1024);
      this.setPosition(tile.getBeamLinkPos(((BlockEntity)linkTarget).getBlockPos()));
      this.terminateSource = tile.renderBeamTermination();
      this.linkTarget = linkTarget.getBeamLinkPos(tile.getBlockPos());
      this.terminateTarget = linkTarget.renderBeamTermination();
      this.setBoundingBox(new AABB(this.x, this.y, this.z, this.linkTarget.x, this.linkTarget.y, this.linkTarget.z));
   }

   @Override
   public void tick() {
      super.tick();
      BCProfiler.TICK.start("crystal_beam_fx_update");
      if (this.ticksTillDeath-- <= 0) {
         this.remove();
      }

      float[] r = new float[]{0.0F, 0.8F, 1.0F};
      float[] g = new float[]{0.8F, 0.1F, 0.7F};
      float[] b = new float[]{1.0F, 1.0F, 0.2F};
      this.rCol = r[this.tile.getTier()];
      this.gCol = g[this.tile.getTier()];
      this.bCol = b[this.tile.getTier()];
      this.powerLevel = (float)MathHelper.approachExp((double)this.powerLevel, (double)this.fxState, 0.05);
      BCProfiler.TICK.stop();
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      if (!(this.powerLevel <= 0.0F) || ClientEventHandler.playerHoldingWrench) {
         BCProfiler.RENDER.start("crystal_beam_fx");
         float scale = 0.1F * this.powerLevel;
         if (ClientEventHandler.playerHoldingWrench) {
            scale = 0.1F;
         }

         Vec3 viewVec = renderInfo.getPosition();
         Vector3 source = new Vector3(this.x - viewVec.x, this.y - viewVec.y, this.z - viewVec.z);
         Vector3 target = this.linkTarget.toVector3().subtract(viewVec.x, viewVec.y, viewVec.z);
         Vector3 dirVec = source.copy().subtract(target).normalize();
         Vector3 planeA = dirVec.copy().perpendicular().normalize();
         Vector3 planeB = dirVec.copy().crossProduct(planeA);
         Vector3 planeC = planeB.copy().rotate(0.7853981633974351, dirVec).normalize();
         Vector3 planeD = planeB.copy().rotate(-0.7853981633974351, dirVec).normalize();
         planeA.multiply((double)scale);
         planeB.multiply((double)scale);
         planeC.multiply((double)scale);
         planeD.multiply((double)scale);
         float dist = 0.2F * (float)Utils.getDistance(new Vec3D(source), new Vec3D(target));
         float anim = ((float)ClientEventHandler.elapsedTicks + partialTicks) / -15.0F;
         float red = 1.0F;
         float green = this.tile.getTier() == 1 ? 0.3F : 1.0F;
         if (ClientEventHandler.playerHoldingWrench) {
            green = 0.0F;
            red = 0.0F;
         }

         Vector3 p1 = source.copy().add(planeA);
         Vector3 p2 = target.copy().add(planeA);
         Vector3 p3 = source.copy().subtract(planeA);
         Vector3 p4 = target.copy().subtract(planeA);
         this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist, red, green);
         p1 = source.copy().add(planeB);
         p2 = target.copy().add(planeB);
         p3 = source.copy().subtract(planeB);
         p4 = target.copy().subtract(planeB);
         this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist, red, green);
         p1 = source.copy().add(planeC);
         p2 = target.copy().add(planeC);
         p3 = source.copy().subtract(planeC);
         p4 = target.copy().subtract(planeC);
         this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist, red, green);
         p1 = source.copy().add(planeD);
         p2 = target.copy().add(planeD);
         p3 = source.copy().subtract(planeD);
         p4 = target.copy().subtract(planeD);
         this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist, red, green);
         scale *= 2.0F;
         float minU = 0.0F;
         float maxU = 0.53F;
         float minV = 0.0F;
         float maxV = 0.53F;
         if (this.terminateSource) {
            float viewX = (float)(this.x - viewVec.x());
            float viewY = (float)(this.y - viewVec.y());
            float viewZ = (float)(this.z - viewVec.z());
            Vector3f[] renderVector = this.getRenderVectors(renderInfo, viewX, viewY, viewZ, scale);
            buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(red, green, 1.0F, 1.0F).setUv(maxU, maxV).setUv2(240, 240);
            buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(red, green, 1.0F, 1.0F).setUv(maxU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(red, green, 1.0F, 1.0F).setUv(minU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(red, green, 1.0F, 1.0F).setUv(minU, maxV).setUv2(240, 240);
         }

         if (this.terminateTarget) {
            float viewX = (float)(this.linkTarget.x - viewVec.x());
            float viewY = (float)(this.linkTarget.y - viewVec.y());
            float viewZ = (float)(this.linkTarget.z - viewVec.z());
            Vector3f[] renderVector = this.getRenderVectors(renderInfo, viewX, viewY, viewZ, scale);
            buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(red, green, 1.0F, 1.0F).setUv(maxU, maxV).setUv2(240, 240);
            buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(red, green, 1.0F, 1.0F).setUv(maxU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(red, green, 1.0F, 1.0F).setUv(minU, minV).setUv2(240, 240);
            buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(red, green, 1.0F, 1.0F).setUv(minU, maxV).setUv2(240, 240);
         }

         BCProfiler.RENDER.stop();
      }
   }

   private void bufferQuad(VertexConsumer buffer, Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4, float anim, float dist, float red, float green) {
      BCProfiler.RENDER.start("buffer_quad");
      buffer.addVertex((float)p1.x, (float)p1.y, (float)p1.z).setColor(red, green, 1.0F, 1.0F).setUv(0.5F, anim).setUv2(240, 240);
      buffer.addVertex((float)p2.x, (float)p2.y, (float)p2.z).setColor(red, green, 1.0F, 1.0F).setUv(0.5F, dist + anim).setUv2(240, 240);
      buffer.addVertex((float)p4.x, (float)p4.y, (float)p4.z).setColor(red, green, 1.0F, 1.0F).setUv(1.0F, dist + anim).setUv2(240, 240);
      buffer.addVertex((float)p3.x, (float)p3.y, (float)p3.z).setColor(red, green, 1.0F, 1.0F).setUv(1.0F, anim).setUv2(240, 240);
      BCProfiler.RENDER.stop();
   }

   public ParticleRenderType getRenderType() {
      return this.tile.getTier() == 0 ? BASIC_HANDLER : (this.tile.getTier() == 1 ? WYVERN_HANDLER : DRACONIC_HANDLER);
   }

   public static class FXHandler implements ParticleRenderType {
      private static final ResourceLocation highlightTexture = ResourceLocation.fromNamespaceAndPath(
         "crystalnetwork", "textures/particle/energy_beam_highlight.png"
      );
      private ResourceLocation texture;
      private float green;

      public FXHandler(ResourceLocation texture) {
         this.texture = texture;
         this.green = texture.getPath().contains("energy_beam_wyvern") ? 0.3F : 1.0F;
      }

      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         if (ClientEventHandler.playerHoldingWrench) {
            RenderSystem.setShaderTexture(0, highlightTexture);
         } else {
            RenderSystem.setShaderTexture(0, this.texture);
         }

         return tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
      }
   }
}
