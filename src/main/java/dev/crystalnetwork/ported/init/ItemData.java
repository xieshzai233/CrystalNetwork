package dev.crystalnetwork.ported.init;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemData {
    private static final DeferredRegister.DataComponents DATA = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "crystalnetwork");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> BINDER_POS = DATA.registerComponentType(
            "binder_pos", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static void register(IEventBus bus) { DATA.register(bus); }
    private ItemData() { }
}
