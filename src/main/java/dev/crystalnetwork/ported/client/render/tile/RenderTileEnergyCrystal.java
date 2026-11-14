package dev.crystalnetwork.ported.client.render.tile;

import codechicken.lib.colour.Colour;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.brandonscore.client.render.RenderUtils;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.client.handler.ClientEventHandler;
import dev.crystalnetwork.ported.client.CrystalShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class RenderTileEnergyCrystal implements BlockEntityRenderer<TileCrystalBase> {
   public static float[][] COLOURS = new float[][]{{0.0F, 0.2F, 0.3F}, {0.47F, 0.0F, 0.58F}, {1.0F, 0.4F, 0.1F}};
   private static final RenderType fallBackType = RenderType.entityTranslucent(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/models/crystal_no_shader.png")
   );
   private static final RenderType fallBackOverlayType = RenderType.entityTranslucent(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/models/crystal_base.png")
   );
   private static final RenderType crystalBaseType = RenderType.entitySolid(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/models/crystal_base.png")
   );
   public static RenderType crystalType = RenderType.create(
      "crystal_type",
      DefaultVertexFormat.POSITION_TEX,
      Mode.QUADS,
      256,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> CrystalShaders.energyCrystalShader))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .createCompositeState(false)
   );
   private final CCModel crystalFull;
   private final CCModel crystalHalf;
   private final CCModel crystalBase;
   private static float[] r = new float[]{0.0F, 0.55F, 1.0F};
   private static float[] g = new float[]{0.35F, 0.3F, 0.572F};
   private static float[] b = new float[]{0.65F, 0.9F, 0.172F};

   public RenderTileEnergyCrystal(Context context) {
      Map<String, CCModel> map = new OBJParser(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "models/block/crystal.obj"))
         .quads()
         .ignoreMtl()
         .parse();
      this.crystalFull = CCModel.combine(map.values());
      map = new OBJParser(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "models/block/crystal_half.obj")).quads().ignoreMtl().parse();
      this.crystalHalf = map.get("Crystal");
      this.crystalBase = map.get("Base");
   }

   public void render(TileCrystalBase te, float partialTicks, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
      int tier = te.getTier();
      Matrix4 mat = new Matrix4(mStack);
      CCRenderState ccrs = CCRenderState.instance();
      ccrs.reset();
      ccrs.brightness = 240;
      ccrs.overlay = packedOverlay;
      Player player = Minecraft.getInstance().player;
      BlockPos pos = te.getBlockPos();
      double x = player.getX() - ((double)pos.getX() + 0.5);
      double y = player.getY() - ((double)pos.getY() + 0.5);
      double z = player.getZ() - ((double)pos.getZ() + 0.5);
      double mm = MathHelper.clip((x * x + y * y + z * z - 5.0) / 4096.0, 0.0, 1.0);
      ccrs.baseColour = -1;
      CrystalShaders.energyCrystalMipmap.glUniform1f((float)mm);
      CrystalShaders.energyCrystalColour.glUniform3f(COLOURS[tier][0], COLOURS[tier][1], COLOURS[tier][2]);
      if (te instanceof TileCrystalDirectIO) {
         ccrs.bind(crystalBaseType, getter);
         mat.translate(0.5, 1.0, 0.5);
         mat.scale(-0.5);
         mat.apply(
            Rotation.sideOrientation(((Direction)((TileCrystalDirectIO)te).facing.get()).getOpposite().get3DDataValue(), 0).at(new Vector3(0.0, 1.0, 0.0))
         );
         this.crystalBase.render(ccrs, new IVertexOperation[]{mat});
         mat.rotate((double)(((float)ClientEventHandler.elapsedTicks + partialTicks) / 400.0F), new Vector3(0.0, 1.0, 0.0));
         if (mm < 1.0) {
            ccrs.bind(crystalType, getter);
            this.crystalHalf.render(ccrs, new IVertexOperation[]{mat});
         } else {
            ccrs.baseColour = Colour.packRGBA((double)r[tier], (double)g[tier], (double)b[tier], 1.0);
            ccrs.bind(fallBackType, getter);
            this.crystalHalf.render(ccrs, new IVertexOperation[]{mat});
            ccrs.baseColour = -1;
         }
      } else {
         mat.translate(Vector3.CENTER);
         mat.rotate(3.1415926535897403, new Vector3(1.0, 0.0, 0.0));
         mat.scale(-0.5);
         mat.rotate((double)(((float)ClientEventHandler.elapsedTicks + partialTicks) / 400.0F), new Vector3(0.0, 1.0, 0.0));
         if (mm < 1.0) {
            ccrs.bind(crystalType, getter);
            this.crystalFull.render(ccrs, new IVertexOperation[]{mat});
         } else {
            ccrs.baseColour = Colour.packRGBA((double)r[tier], (double)g[tier], (double)b[tier], 1.0);
            ccrs.bind(fallBackType, getter);
            this.crystalFull.render(ccrs, new IVertexOperation[]{mat});
            ccrs.baseColour = -1;
         }
      }

      RenderUtils.endBatch(getter);
   }
}
