package dev.crystalnetwork.ported.client.render.item;

import codechicken.lib.colour.Colour;
import codechicken.lib.model.PerspectiveModelState;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.item.IItemRenderer;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.brandonscore.api.TimeKeeper;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.client.CrystalShaders;
import dev.crystalnetwork.ported.client.render.tile.RenderTileEnergyCrystal;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RenderItemEnergyCrystal implements IItemRenderer {
   public static final RenderType crystalBaseType = RenderType.entitySolid(
      ResourceLocation.fromNamespaceAndPath("crystalnetwork", "textures/models/crystal_base.png")
   );
   private final EnergyCrystal.CrystalType type;
   private final TechLevel techLevel;
   private final CCModel crystalFull;
   private final CCModel crystalHalf;
   private final CCModel crystalBase;
   private static float[] r = new float[]{0.0F, 0.47F, 1.0F};
   private static float[] g = new float[]{0.2F, 0.0F, 0.4F};
   private static float[] b = new float[]{0.3F, 0.58F, 0.1F};

   public RenderItemEnergyCrystal(EnergyCrystal.CrystalType type, TechLevel techLevel) {
      this.type = type;
      this.techLevel = techLevel;
      Map<String, CCModel> map = new OBJParser(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "models/block/crystal.obj"))
         .quads()
         .ignoreMtl()
         .parse();
      this.crystalFull = CCModel.combine(map.values()).backfacedCopy();
      map = new OBJParser(ResourceLocation.fromNamespaceAndPath("crystalnetwork", "models/block/crystal_half.obj")).quads().ignoreMtl().parse();
      this.crystalHalf = map.get("Crystal").backfacedCopy();
      this.crystalBase = map.get("Base").backfacedCopy();
   }

   public boolean useAmbientOcclusion() {
      return false;
   }

   public boolean isGui3d() {
      return false;
   }

   public boolean usesBlockLight() {
      return false;
   }

   public void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack mStack, MultiBufferSource getter, int packedLight, int packedOverlay) {
      int tier = this.techLevel.index;
      Matrix4 mat = new Matrix4(mStack);
      CCRenderState ccrs = CCRenderState.instance();
      ccrs.reset();
      ccrs.brightness = packedLight;
      ccrs.overlay = packedOverlay;
      mat.translate(0.5, this.type == EnergyCrystal.CrystalType.CRYSTAL_IO ? 0.0 : 0.5, 0.5);
      CrystalShaders.energyCrystalMipmap.glUniform1f(0.0F);
      CrystalShaders.energyCrystalColour.glUniform3f(RenderTileEnergyCrystal.COLOURS[tier][0],
              RenderTileEnergyCrystal.COLOURS[tier][1], RenderTileEnergyCrystal.COLOURS[tier][2]);
      if (this.type == EnergyCrystal.CrystalType.CRYSTAL_IO) {
         ccrs.bind(crystalBaseType, getter);
         this.crystalBase.render(ccrs, new IVertexOperation[]{mat});
         mat.apply(new Rotation((double)((float)TimeKeeper.getClientTick() / 400.0F), 0.0, 1.0, 0.0));
         ccrs.baseColour = Colour.packRGBA((double)r[tier], (double)g[tier], (double)b[tier], 1.0);
         ccrs.bind(RenderTileEnergyCrystal.crystalType, getter);
         this.crystalHalf.render(ccrs, new IVertexOperation[]{mat});
      } else {
         ccrs.baseColour = Colour.packRGBA((double)r[tier], (double)g[tier], (double)b[tier], 1.0);
         mat.apply(new Rotation((double)((float)TimeKeeper.getClientTick() / 400.0F), 0.0, 1.0, 0.0));
         ccrs.bind(RenderTileEnergyCrystal.crystalType, getter);
         this.crystalFull.render(ccrs, new IVertexOperation[]{mat});
      }
   }

   @Nullable
   public PerspectiveModelState getModelState() {
      return TransformUtils.DEFAULT_BLOCK;
   }

   public static class ITEM_BASIC_IO_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_BASIC_IO_CRYSTAL() {
         super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.DRACONIUM);
      }
   }

   public static class ITEM_BASIC_RELAY_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_BASIC_RELAY_CRYSTAL() {
         super(EnergyCrystal.CrystalType.RELAY, TechLevel.DRACONIUM);
      }
   }

   public static class ITEM_BASIC_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_BASIC_WIRELESS_CRYSTAL() {
         super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.DRACONIUM);
      }
   }

   public static class ITEM_DRACONIC_IO_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_DRACONIC_IO_CRYSTAL() {
         super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.DRACONIC);
      }
   }

   public static class ITEM_DRACONIC_RELAY_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_DRACONIC_RELAY_CRYSTAL() {
         super(EnergyCrystal.CrystalType.RELAY, TechLevel.DRACONIC);
      }
   }

   public static class ITEM_DRACONIC_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_DRACONIC_WIRELESS_CRYSTAL() {
         super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.DRACONIC);
      }
   }

   public static class ITEM_WYVERN_IO_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_WYVERN_IO_CRYSTAL() {
         super(EnergyCrystal.CrystalType.CRYSTAL_IO, TechLevel.WYVERN);
      }
   }

   public static class ITEM_WYVERN_RELAY_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_WYVERN_RELAY_CRYSTAL() {
         super(EnergyCrystal.CrystalType.RELAY, TechLevel.WYVERN);
      }
   }

   public static class ITEM_WYVERN_WIRELESS_CRYSTAL extends RenderItemEnergyCrystal {
      public ITEM_WYVERN_WIRELESS_CRYSTAL() {
         super(EnergyCrystal.CrystalType.WIRELESS, TechLevel.WYVERN);
      }
   }
}
