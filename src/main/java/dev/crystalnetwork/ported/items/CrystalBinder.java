package dev.crystalnetwork.ported.items;

import dev.crystalnetwork.ported.api.energy.ICrystalBinder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class CrystalBinder extends Item implements ICrystalBinder {
   public CrystalBinder(Properties properties) {
      super(properties);
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

}
