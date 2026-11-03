package io.vinum.bar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class BarContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "vinum");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinum");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "vinum");
    public static final DeferredHolder<Block, BarBlock> BAR = BLOCKS.register("bar", () -> new BarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.register("bar", () -> new BlockItem(BAR.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarBlockEntity>> ENTITY = ENTITIES.register("bar", () -> BlockEntityType.Builder.of(BarBlockEntity::new, BAR.get()).build(null));
    private BarContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == io.vinum.item.ModItemGroup.MAIN.get()) event.accept(ITEM.get());
        });
    }
}
