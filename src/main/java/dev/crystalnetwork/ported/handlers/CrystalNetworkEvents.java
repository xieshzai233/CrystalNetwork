package dev.crystalnetwork.ported.handlers;

import codechicken.lib.raytracer.RayTracer;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.items.CrystalBinder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CrystalNetworkEvents {
    private static final TagKey<Item> COMMON_WRENCH = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));
    private static final TagKey<Item> FORGE_WRENCH = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "tools/wrench"));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        TileCrystalRelay.register(event);
        TileCrystalDirectIO.register(event);
        TileCrystalWirelessIO.register(event);
    }

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (event.getEntity().isShiftKeyDown()
                && !(stack.getItem() instanceof CrystalBinder)
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof EnergyCrystal
                && isExternalWrenchOrConfigurator(stack)) {
            if (!event.getLevel().isClientSide) {
                event.getLevel().destroyBlock(event.getPos(), true, event.getEntity());
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            return;
        }
        if (event.getHand() == InteractionHand.OFF_HAND
                && event.getEntity().getMainHandItem().getItem() instanceof CrystalBinder) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            return;
        }
        if (stack.getItem() instanceof CrystalBinder && BinderHandler.onBinderUse(
                event.getEntity(), event.getHand(), event.getLevel(), event.getPos(), stack, event.getFace())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
        }
    }

    private static boolean isExternalWrenchOrConfigurator(ItemStack stack) {
        if (stack.is(COMMON_WRENCH) || stack.is(FORGE_WRENCH)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.contains("wrench") || path.contains("configurator") || path.contains("configuration_tool");
    }

    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isShiftKeyDown() && event.getItemStack().getItem() instanceof CrystalBinder
                && RayTracer.retrace(event.getEntity()).getType() == HitResult.Type.MISS) {
            if (event.getLevel().isClientSide || BinderHandler.clearBinder(event.getEntity(), event.getItemStack())) {
                event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
                event.setCanceled(true);
            }
        }
    }

    private CrystalNetworkEvents() { }
}
