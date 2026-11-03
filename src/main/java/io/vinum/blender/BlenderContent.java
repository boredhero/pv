package io.vinum.blender;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Cold two-input mixing machine; all processing and sided access live in the block entity. */
public final class BlenderContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "vinum");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinum");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "vinum");
    public static final DeferredHolder<Block, BlenderBlock> BLENDER = BLOCKS.register("blender", () ->
        new BlenderBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.register("blender", () ->
        new BlockItem(BLENDER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlenderBlockEntity>> ENTITY = ENTITIES.register("blender", () ->
        BlockEntityType.Builder.of(BlenderBlockEntity::new, BLENDER.get()).build(null));
    private BlenderContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                ENTITY.get(), (blender, side) -> blender.fluids(side));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ENTITY.get(), (blender, side) -> blender.items(side));
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == io.vinum.item.ModItemGroup.MAIN.get()) event.accept(ITEM.get());
        });
    }
}
