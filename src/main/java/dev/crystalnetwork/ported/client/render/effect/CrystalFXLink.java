package dev.crystalnetwork.ported.client.render.effect;

import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.lib.Vec3D;
import com.brandon3055.brandonscore.utils.Utils;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CrystalFXLink extends CrystalFXBase<TileCrystalBase> {
   private final Vec3D linkTarget;
   private final boolean terminateSource;
   private final boolean terminateTarget;
   public int timeout = 0;
   private static final ParticleRenderType HANDLER = new CrystalFXLink.FXHandler();

   public CrystalFXLink(ClientLevel worldIn, TileCrystalBase tile, Vec3D linkTarget) {
      super(worldIn, tile);
      this.age = worldIn.random.nextInt(1024);
      this.setPosition(tile.getBeamLinkPos(linkTarget.getPos()));
      this.terminateSource = true;
      this.linkTarget = linkTarget;
      Direction face = tile instanceof TileCrystalWirelessIO ? ((TileCrystalWirelessIO)tile).getReceiversFaces().get(linkTarget.getPos()) : null;
      if (face != null) {
         linkTarget.add((double)face.getStepX() * 0.6, (double)face.getStepY() * 0.6, (double)face.getStepZ() * 0.6);
      }

      this.terminateTarget = true;
      this.setBoundingBox(new AABB(this.x, this.y, this.z, this.linkTarget.x, this.linkTarget.y, this.linkTarget.z));
   }

   @Override
   public void tick() {
      super.tick();
      if (!ClientEventHandler.playerHoldingWrench && this.timeout <= 0) {
         this.remove();
      } else if (this.timeout > 0) {
         this.timeout--;
      }
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      float scale = 0.1F + (float)this.timeout * 0.005F;
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
      Vector3 p1 = source.copy().add(planeA);
      Vector3 p2 = target.copy().add(planeA);
      Vector3 p3 = source.copy().subtract(planeA);
      Vector3 p4 = target.copy().subtract(planeA);
      this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist);
      p1 = source.copy().add(planeB);
      p2 = target.copy().add(planeB);
      p3 = source.copy().subtract(planeB);
      p4 = target.copy().subtract(planeB);
      this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist);
      p1 = source.copy().add(planeC);
      p2 = target.copy().add(planeC);
      p3 = source.copy().subtract(planeC);
      p4 = target.copy().subtract(planeC);
      this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist);
      p1 = source.copy().add(planeD);
      p2 = target.copy().add(planeD);
      p3 = source.copy().subtract(planeD);
      p4 = target.copy().subtract(planeD);
      this.bufferQuad(buffer, p1, p2, p3, p4, anim, dist);
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
         buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(maxU, maxV).setUv2(240, 240);
         buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(maxU, minV).setUv2(240, 240);
         buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(minU, minV).setUv2(240, 240);
         buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(minU, maxV).setUv2(240, 240);
      }

      if (this.terminateTarget) {
         float viewX = (float)(this.linkTarget.x - viewVec.x());
         float viewY = (float)(this.linkTarget.y - viewVec.y());
         float viewZ = (float)(this.linkTarget.z - viewVec.z());
         Vector3f[] renderVector = this.getRenderVectors(renderInfo, viewX, viewY, viewZ, scale);
         buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(maxU, maxV).setUv2(240, 240);
         buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(maxU, minV).setUv2(240, 240);
         buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(minU, minV).setUv2(240, 240);
         buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z()).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(minU, maxV).setUv2(240, 240);
      }
   }

   private void bufferQuad(VertexConsumer buffer, Vector3 p1, Vector3 p2, Vector3 p3, Vector3 p4, float anim, float dist) {
      buffer.addVertex((float)p1.x, (float)p1.y, (float)p1.z).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(0.5F, anim).setUv2(240, 240);
      buffer.addVertex((float)p2.x, (float)p2.y, (float)p2.z).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(0.5F, dist + anim).setUv2(240, 240);
      buffer.addVertex((float)p4.x, (float)p4.y, (float)p4.z).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(1.0F, dist + anim).setUv2(240, 240);
      buffer.addVertex((float)p3.x, (float)p3.y, (float)p3.z).setColor(1.0F, 0.0F, 0.0F, 1.0F).setUv(1.0F, anim).setUv2(240, 240);
   }

   public ParticleRenderType getRenderType() {
      return HANDLER;
   }

   public static class FXHandler implements ParticleRenderType {
      private static final ResourceLocation highlightTexture = ResourceLocation.fromNamespaceAndPath(
         "crystalnetwork", "textures/particle/energy_beam_highlight.png"
      );

      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
         RenderSystem.setShaderTexture(0, highlightTexture);
         return tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
      }
   }
}
