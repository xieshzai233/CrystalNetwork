package dev.crystalnetwork.ported.handlers;

import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.items.CrystalBinder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CrystalNetworkEvents {
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        TileCrystalRelay.register(event);
        TileCrystalDirectIO.register(event);
        TileCrystalWirelessIO.register(event);
    }

    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.OFF_HAND) return;
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof CrystalBinder && BinderHandler.onBinderUse(
                event.getEntity(), event.getHand(), event.getLevel(), event.getPos(), stack, event.getFace())) {
            event.setCanceled(true);
        }
    }

    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide && event.getEntity().isShiftKeyDown() && event.getItemStack().getItem() instanceof CrystalBinder
                && BinderHandler.clearBinder(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    private CrystalNetworkEvents() { }
}
