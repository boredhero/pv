package io.vinum.shelf;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ShelfContent {
  public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "vinum");
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinum");
  public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "vinum");
  public static final DeferredHolder<Block, WineShelfBlock> SHELF = BLOCKS.register("wine_shelf", () -> new WineShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
  public static final DeferredItem<BlockItem> ITEM = ITEMS.register("wine_shelf", () -> new BlockItem(SHELF.get(), new Item.Properties()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WineShelfBlockEntity>> ENTITY = ENTITIES.register("wine_shelf", () -> BlockEntityType.Builder.of(WineShelfBlockEntity::new, SHELF.get()).build(null));
  private ShelfContent() {}
  public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); }
}
