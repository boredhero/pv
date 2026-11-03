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
    public static final DeferredHolder<Block, SugarKettleBlock> KETTLE = BLOCKS.register("sugar_kettle", () -> new SugarKettleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.register("sugar_kettle", () -> new SugarKettleItem(KETTLE.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BAGASSE = ITEMS.register("bagasse", () -> new Item(new Item.Properties()) {
        @Override public int getBurnTime(ItemStack stack, net.minecraft.world.item.crafting.RecipeType<?> recipeType) { return 100; }
    });
    public static final DeferredItem<Item> PANELA = ITEMS.registerSimpleItem("panela");
    public static final DeferredItem<Item> SEPARATION_INSERT = ITEMS.registerSimpleItem("sugar_separation_insert", new Item.Properties().stacksTo(1));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SugarKettleBlockEntity>> ENTITY = ENTITIES.register("sugar_kettle", () -> BlockEntityType.Builder.of(SugarKettleBlockEntity::new, KETTLE.get()).build(null));
    private SugarContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == io.vinum.item.ModItemGroup.MAIN.get()) {
                event.accept(ITEM.get()); event.accept(BAGASSE.get()); event.accept(PANELA.get()); event.accept(SEPARATION_INSERT.get());
            }
        });
    }
}
