package io.vinum.sugar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class SugarContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "vinum");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinum");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "vinum");
    public static final DeferredHolder<Block, SugarPlantBlock> PLANT = BLOCKS.register("sugar_plant", () -> new SugarPlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> PLANT_ITEM = ITEMS.register("sugar_plant", () -> new BlockItem(PLANT.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BAGASSE = ITEMS.register("bagasse", () -> new Item(new Item.Properties()) {
        @Override public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType) { return 100; }
    });
    public static final DeferredItem<Item> PANELA = ITEMS.registerSimpleItem("panela");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SugarPlantBlockEntity>> PLANT_ENTITY = ENTITIES.register("sugar_plant", () -> BlockEntityType.Builder.of(SugarPlantBlockEntity::new, PLANT.get()).build(null));
    private SugarContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) -> {
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                PLANT_ENTITY.get(), (plant, side) -> plant.fluids(side));
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                PLANT_ENTITY.get(), (plant, side) -> plant.items(side));
        });
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == io.vinum.item.ModItemGroup.MAIN.get()) {
                event.accept(PLANT_ITEM.get()); event.accept(BAGASSE.get()); event.accept(PANELA.get());
            }
        });
    }
}
