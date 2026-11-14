package dev.crystalnetwork.ported.handlers;

import codechicken.lib.raytracer.RayTracer;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.items.CrystalBinder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CrystalNetworkEvents {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        TileCrystalRelay.register(event);
        TileCrystalDirectIO.register(event);
        TileCrystalWirelessIO.register(event);
    }

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.OFF_HAND
                && event.getEntity().getMainHandItem().getItem() instanceof CrystalBinder) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof CrystalBinder && BinderHandler.onBinderUse(
                event.getEntity(), event.getHand(), event.getLevel(), event.getPos(), stack, event.getFace())) {
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            event.setCanceled(true);
        }
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
