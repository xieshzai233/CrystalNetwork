package dev.crystalnetwork.ported.client.render.effect;

import codechicken.lib.math.MathHelper;
import codechicken.lib.vec.Vector3;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.client.AtlasTextureHelper;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class CrystalFXWireless extends CrystalFXBase<TileCrystalWirelessIO> {
   private final BlockPos linkTarget;
   private final AABB targetBB;
   private float powerLevel = 0.0F;
   private List<CrystalFXWireless.PTracker> trackers = new ArrayList<>();
   private static final ParticleRenderType BASIC_HANDLER = new CrystalFXWireless.FXHandler();
   private static final ParticleRenderType WYVERN_HANDLER = new CrystalFXWireless.FXHandler();
   private static final ParticleRenderType DRACONIC_HANDLER = new CrystalFXWireless.FXHandler();

   public CrystalFXWireless(ClientLevel worldIn, TileCrystalWirelessIO tile, BlockPos linkTarget) {
      super(worldIn, tile);
      this.age = worldIn.random.nextInt(1024);
      this.setPosition(tile.getBeamLinkPos(linkTarget));
      this.linkTarget = linkTarget;
      BlockState state = worldIn.getBlockState(linkTarget);
      VoxelShape shape = state.getShape(worldIn, linkTarget);
      if (shape.isEmpty()) {
         shape = Shapes.block();
      }

      this.targetBB = shape.bounds();
      this.targetBB.deflate(0.05);
      this.setBoundingBox(new AABB(this.x, this.y, this.z, (double)this.linkTarget.getX(), (double)this.linkTarget.getY(), (double)this.linkTarget.getZ()));
   }

   @Override
   public void tick() {
      super.tick();
      if (this.ticksTillDeath-- <= 0) {
         this.remove();
      }

      this.rCol = this.gCol = this.bCol = this.alpha = 1.0F;
      this.powerLevel = (float)MathHelper.approachExp((double)this.powerLevel, (double)this.fxState, 0.05);
      Iterator<CrystalFXWireless.PTracker> i = this.trackers.iterator();

      while (i.hasNext()) {
         CrystalFXWireless.PTracker next = i.next();
         if (next.ticksActive >= next.travelTime) {
            i.remove();
         } else {
            next.ticksActive++;
         }
      }

      int ps = ((ParticleStatus)Minecraft.getInstance().options.particles().get()).getId();
      if (this.age % 2 == 0
         && this.powerLevel > this.random.nextFloat()
         && (ps == 0 || ps == 1 && this.random.nextInt(3) == 0 || ps == 2 && this.random.nextInt(10) == 0)) {
         double travel = (double)(50 + this.random.nextInt(50));
         travel *= (double)(1.4F - this.powerLevel);
         this.trackers
            .add(
               new CrystalFXWireless.PTracker(
                  (int)travel,
                  new Vector3(
                     this.targetBB.minX + this.random.nextDouble() * (this.targetBB.maxX - this.targetBB.minX),
                     this.targetBB.minY + this.random.nextDouble() * (this.targetBB.maxY - this.targetBB.minY),
                     this.targetBB.minZ + this.random.nextDouble() * (this.targetBB.maxZ - this.targetBB.minZ)
                  )
               )
            );
      }

      this.age++;
   }

   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      int texIndex = ClientEventHandler.elapsedTicks % AtlasTextureHelper.ENERGY_PARTICLE.length;
      TextureAtlasSprite sprite = AtlasTextureHelper.ENERGY_PARTICLE[texIndex];
      if (sprite != null) {
         float minU = sprite.getU0();
         float maxU = sprite.getU1();
         float minV = sprite.getV0();
         float maxV = sprite.getV1();
         float scale = 0.08F;
         boolean output = !this.tile.inputMode.get();
         Vec3 view = renderInfo.getPosition();
         Vector3 source = new Vector3(this.x - view.x, this.y - view.y, this.z - view.z);
         Vector3 target = Vector3.fromBlockPos(this.linkTarget).subtract(view.x, view.y, view.z);

         for (CrystalFXWireless.PTracker tracker : this.trackers) {
            double progress = ((double)tracker.ticksActive + (double)partialTicks) / (double)tracker.travelTime;
            if (!output) {
               progress = 1.0 - progress;
            }

            if (!(progress >= 1.0) && !(progress <= 0.0)) {
               Vector3 pathVec = target.copy().subtract(source);
               pathVec.add(tracker.tOffset);
               pathVec.multiply(progress);
               pathVec.add(source);
               Vector3f[] renderVector = this.getRenderVectors(renderInfo, (float)pathVec.x, (float)pathVec.y, (float)pathVec.z, scale);
               buffer.addVertex(renderVector[0].x(), renderVector[0].y(), renderVector[0].z())
                  .setColor(0.0F, 1.0F, 1.0F, 1.0F)
                  .setUv(maxU, maxV)
                  .setUv2(240, 240);
               buffer.addVertex(renderVector[1].x(), renderVector[1].y(), renderVector[1].z())
                  .setColor(0.0F, 1.0F, 1.0F, 1.0F)
                  .setUv(maxU, minV)
                  .setUv2(240, 240);
               buffer.addVertex(renderVector[2].x(), renderVector[2].y(), renderVector[2].z())
                  .setColor(0.0F, 1.0F, 1.0F, 1.0F)
                  .setUv(minU, minV)
                  .setUv2(240, 240);
               buffer.addVertex(renderVector[3].x(), renderVector[3].y(), renderVector[3].z())
                  .setColor(0.0F, 1.0F, 1.0F, 1.0F)
                  .setUv(minU, maxV)
                  .setUv2(240, 240);
            }
         }
      }
   }

   public ParticleRenderType getRenderType() {
      return this.tile.getTier() == 0 ? BASIC_HANDLER : (this.tile.getTier() == 1 ? WYVERN_HANDLER : DRACONIC_HANDLER);
   }

   public static class FXHandler implements ParticleRenderType {
      public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
         RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
         return tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
      }
   }

   public static class PTracker {
      public int ticksActive = 0;
      public final int travelTime;
      public final Vector3 tOffset;

      public PTracker(int travelTime, Vector3 tOffset) {
         this.travelTime = travelTime;
         this.tOffset = tOffset;
      }
   }
}
