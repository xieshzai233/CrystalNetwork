package dev.crystalnetwork.ported.items.tools;

import dev.crystalnetwork.ported.api.energy.ICrystalBinder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class CrystalBinder extends Item implements ICrystalBinder {
   public CrystalBinder(Properties properties) {
      super(properties);
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState rotated = state.rotate(world, pos, Rotation.CLOCKWISE_90);
      if (!rotated.equals(state)) {
         world.setBlockAndUpdate(pos, rotated);
         return world.isClientSide ? InteractionResult.PASS : InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }
}
