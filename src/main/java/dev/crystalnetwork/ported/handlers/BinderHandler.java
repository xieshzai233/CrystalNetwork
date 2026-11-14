package dev.crystalnetwork.ported.handlers;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.vec.Cuboid6;
import com.brandon3055.brandonscore.lib.ChatHelper;
import dev.crystalnetwork.ported.api.energy.ICrystalLink;
import dev.crystalnetwork.ported.api.render.DERenderTypes;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalBase;
import dev.crystalnetwork.ported.init.ItemData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BinderHandler {
   public static Map<AABB, CCModel> modelCache = new HashMap<>();

   public static boolean onBinderUse(Player player, InteractionHand hand, Level level, BlockPos blockClicked, @Nonnull ItemStack binder, Direction sideClicked) {
      BlockEntity tile = level.getBlockEntity(blockClicked);
      boolean isBound = isBound(binder);
      // Predict the consumed interaction, but only the server changes bindings and links.
      if (level.isClientSide) {
         return tile instanceof ICrystalLink || isBound;
      }
      if (tile instanceof ICrystalLink && player.isShiftKeyDown()) {
         bind(binder, blockClicked, level);
         ChatHelper.sendIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.pos_saved_to_tool").withStyle(ChatFormatting.GREEN), TileCrystalBase.MSG_ID
         );

         return true;
      } else if (tile instanceof ICrystalLink && !isBound) {
         ChatHelper.sendIndexed(
            player, Component.translatable("gui.crystalnetwork.energy_net.tool_not_bound").withStyle(ChatFormatting.RED), TileCrystalBase.MSG_ID
         );
         return true;
      } else if (isBound) {
         GlobalPos boundPos = getBound(binder);
         if (!boundPos.dimension().equals(level.dimension())) {
            ChatHelper.sendIndexed(
               player, Component.translatable("gui.crystalnetwork.energy_net.link_to_other_dimension").withStyle(ChatFormatting.RED), TileCrystalBase.MSG_ID
            );
            return true;
         } else {
            BlockPos boundLinkable = boundPos.pos();
            if (boundLinkable.equals(blockClicked)) {
               ChatHelper.sendIndexed(
                  player, Component.translatable("gui.crystalnetwork.energy_net.link_to_self").withStyle(ChatFormatting.RED), TileCrystalBase.MSG_ID
               );
               return true;
            } else {
               BlockEntity boundTile = level.getBlockEntity(boundLinkable);
               if (boundTile instanceof ICrystalLink) {
                  if (((ICrystalLink)boundTile).binderUsed(player, blockClicked, sideClicked)) {
                     player.swing(hand);
                  }
               } else {
                  ChatHelper.sendIndexed(
                     player, Component.translatable("gui.crystalnetwork.energy_net.bound_to_invalid").withStyle(ChatFormatting.RED), TileCrystalBase.MSG_ID
                  );
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static boolean isBound(ItemStack stack) {
      return stack.has(ItemData.BINDER_POS);
   }

   private static void bind(ItemStack stack, BlockPos pos, Level level) {
      stack.set(ItemData.BINDER_POS, GlobalPos.of(level.dimension(), pos));
   }

   private static GlobalPos getBound(ItemStack stack) {
      return (GlobalPos)stack.get(ItemData.BINDER_POS);
   }

   public static boolean clearBinder(Player player, @Nonnull ItemStack stack) {
      if (isBound(stack)) {
         stack.remove(ItemData.BINDER_POS);
         ChatHelper.sendIndexed(player, Component.translatable("gui.crystalnetwork.energy_net.pos_cleared"), TileCrystalBase.MSG_ID);
         return true;
      } else {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void renderWorldOverlay(LocalPlayer player, PoseStack pStack, Level level, ItemStack stack, Minecraft mc, float partialTicks) {
      if (isBound(stack)) {
         GlobalPos gPos = getBound(stack);
         if (gPos.dimension() == level.dimension()) {
            BlockPos pos = gPos.pos();
            boolean valid = level.getBlockEntity(pos) instanceof ICrystalLink;
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getShape(level, pos);
            if (shape.isEmpty()) {
               shape = Shapes.block();
            }

            Cuboid6 cuboid6 = new Cuboid6(shape.bounds());
            BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 cameraPos = camera.getPosition();
            pStack.pushPose();
            pStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            pStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
            RenderUtils.bufferCuboidSolid(
               new TransformingVertexConsumer(source.getBuffer(DERenderTypes.BOX_NO_DEPTH), pStack),
               cuboid6,
               valid ? 0.0F : 1.0F,
               valid ? 1.0F : 0.0F,
               0.0F,
               0.5F
            );
            source.endBatch();
            RenderUtils.bufferCuboidOutline(
               new TransformingVertexConsumer(source.getBuffer(DERenderTypes.OUTLINE_TYPE), pStack), cuboid6, 0.0F, 0.0F, 0.0F, 1.0F
            );
            source.endBatch();
            pStack.popPose();
         }
      }
   }

   private static CCModel modelForAABB(AABB aabb) {
      if (!modelCache.containsKey(aabb)) {
         modelCache.put(aabb, CCModel.newModel(Mode.QUADS, 24).generateBlock(0, new Cuboid6(aabb)));
      }

      return modelCache.get(aabb);
   }
}
