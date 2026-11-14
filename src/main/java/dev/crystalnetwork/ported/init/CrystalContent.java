package dev.crystalnetwork.ported.init;

import com.brandon3055.brandonscore.api.TechLevel;
import dev.crystalnetwork.CrystalNetwork;
import dev.crystalnetwork.ported.blocks.energynet.EnergyCrystal;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalDirectIO;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalRelay;
import dev.crystalnetwork.ported.blocks.energynet.tileentity.TileCrystalWirelessIO;
import dev.crystalnetwork.ported.items.CrystalBinder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrystalContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CrystalNetwork.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, CrystalNetwork.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CrystalNetwork.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrystalNetwork.MOD_ID);

    private static final BlockBehaviour.Properties BASIC = BlockBehaviour.Properties.of().strength(3.0F, 8.0F).noOcclusion();
    private static final BlockBehaviour.Properties WYVERN = BlockBehaviour.Properties.of().strength(5.0F, 16.0F).noOcclusion();
    private static final BlockBehaviour.Properties DRACONIC = BlockBehaviour.Properties.of().strength(8.0F, 32.0F).noOcclusion();

    public static final DeferredHolder<Block, EnergyCrystal> BASIC_IO = crystal("basic_io_crystal", BASIC, TechLevel.DRACONIUM, EnergyCrystal.CrystalType.CRYSTAL_IO);
    public static final DeferredHolder<Block, EnergyCrystal> WYVERN_IO = crystal("wyvern_io_crystal", WYVERN, TechLevel.WYVERN, EnergyCrystal.CrystalType.CRYSTAL_IO);
    public static final DeferredHolder<Block, EnergyCrystal> DRACONIC_IO = crystal("draconic_io_crystal", DRACONIC, TechLevel.DRACONIC, EnergyCrystal.CrystalType.CRYSTAL_IO);
    public static final DeferredHolder<Block, EnergyCrystal> BASIC_RELAY = crystal("basic_relay_crystal", BASIC, TechLevel.DRACONIUM, EnergyCrystal.CrystalType.RELAY);
    public static final DeferredHolder<Block, EnergyCrystal> WYVERN_RELAY = crystal("wyvern_relay_crystal", WYVERN, TechLevel.WYVERN, EnergyCrystal.CrystalType.RELAY);
    public static final DeferredHolder<Block, EnergyCrystal> DRACONIC_RELAY = crystal("draconic_relay_crystal", DRACONIC, TechLevel.DRACONIC, EnergyCrystal.CrystalType.RELAY);
    public static final DeferredHolder<Block, EnergyCrystal> BASIC_WIRELESS = crystal("basic_wireless_crystal", BASIC, TechLevel.DRACONIUM, EnergyCrystal.CrystalType.WIRELESS);
    public static final DeferredHolder<Block, EnergyCrystal> WYVERN_WIRELESS = crystal("wyvern_wireless_crystal", WYVERN, TechLevel.WYVERN, EnergyCrystal.CrystalType.WIRELESS);
    public static final DeferredHolder<Block, EnergyCrystal> DRACONIC_WIRELESS = crystal("draconic_wireless_crystal", DRACONIC, TechLevel.DRACONIC, EnergyCrystal.CrystalType.WIRELESS);

    public static final DeferredHolder<Item, Item> CRYSTAL_BINDER = ITEMS.register("crystal_binder", () -> new CrystalBinder(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystalRelay>> TILE_RELAY_CRYSTAL = BLOCK_ENTITIES.register(
            "relay_crystal", () -> BlockEntityType.Builder.of(TileCrystalRelay::new, BASIC_RELAY.get(), WYVERN_RELAY.get(), DRACONIC_RELAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystalDirectIO>> TILE_IO_CRYSTAL = BLOCK_ENTITIES.register(
            "io_crystal", () -> BlockEntityType.Builder.of(TileCrystalDirectIO::new, BASIC_IO.get(), WYVERN_IO.get(), DRACONIC_IO.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileCrystalWirelessIO>> TILE_WIRELESS_CRYSTAL = BLOCK_ENTITIES.register(
            "wireless_crystal", () -> BlockEntityType.Builder.of(TileCrystalWirelessIO::new, BASIC_WIRELESS.get(), WYVERN_WIRELESS.get(), DRACONIC_WIRELESS.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.crystalnetwork"))
            .icon(() -> BASIC_RELAY.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(BASIC_RELAY.get()); output.accept(BASIC_IO.get()); output.accept(BASIC_WIRELESS.get());
                output.accept(WYVERN_RELAY.get()); output.accept(WYVERN_IO.get()); output.accept(WYVERN_WIRELESS.get());
                output.accept(DRACONIC_RELAY.get()); output.accept(DRACONIC_IO.get()); output.accept(DRACONIC_WIRELESS.get());
                output.accept(CRYSTAL_BINDER.get());
            }).build());

    private static DeferredHolder<Block, EnergyCrystal> crystal(String id, BlockBehaviour.Properties properties, TechLevel tier, EnergyCrystal.CrystalType type) {
        DeferredHolder<Block, EnergyCrystal> block = BLOCKS.register(id, () -> new EnergyCrystal(properties, tier, type));
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }

    private CrystalContent() { }
}
