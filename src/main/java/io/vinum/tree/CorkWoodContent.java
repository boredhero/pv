package io.vinum.tree;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Ordinary construction wood shares vanilla mechanics; cork peeling remains on the living log. */
public final class CorkWoodContent {
  public static final DeferredHolder<Block, Block> PLANKS =
      register("planks", () -> new Block(copy(Blocks.OAK_PLANKS)));
  public static final DeferredHolder<Block, Block> STAIRS =
      register(
          "stairs",
          () -> new StairBlock(PLANKS.get().defaultBlockState(), copy(Blocks.OAK_STAIRS)));
  public static final DeferredHolder<Block, Block> SLAB =
      register("slab", () -> new SlabBlock(copy(Blocks.OAK_SLAB)));
  public static final DeferredHolder<Block, Block> FENCE =
      register("fence", () -> new FenceBlock(copy(Blocks.OAK_FENCE)));
  public static final DeferredHolder<Block, Block> FENCE_GATE =
      register("fence_gate", () -> new FenceGateBlock(WoodType.OAK, copy(Blocks.OAK_FENCE_GATE)));
  public static final DeferredHolder<Block, Block> DOOR =
      register("door", () -> new DoorBlock(BlockSetType.OAK, copy(Blocks.OAK_DOOR)));
  public static final DeferredHolder<Block, Block> TRAPDOOR =
      register("trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, copy(Blocks.OAK_TRAPDOOR)));
  public static final DeferredHolder<Block, Block> BUTTON =
      register("button", () -> new ButtonBlock(BlockSetType.OAK, 30, copy(Blocks.OAK_BUTTON)));
  public static final DeferredHolder<Block, Block> PRESSURE_PLATE =
      register(
          "pressure_plate",
          () -> new PressurePlateBlock(BlockSetType.OAK, copy(Blocks.OAK_PRESSURE_PLATE)));

  private CorkWoodContent() {}

  private static BlockBehaviour.Properties copy(Block block) {
    return BlockBehaviour.Properties.ofFullCopy(block);
  }

  private static DeferredHolder<Block, Block> register(String suffix, Supplier<Block> block) {
    String name = "cork_oak_" + suffix;
    var result = CorkOakContent.BLOCKS.register(name, block);
    CorkOakContent.ITEMS.register(name, () -> new BlockItem(result.get(), new Item.Properties()));
    return result;
  }

  public static void register(IEventBus bus) {
    bus.addListener(CorkWoodContent::setup);
  }

  private static void setup(FMLCommonSetupEvent event) {
    event.enqueueWork(
        () -> {
          FireBlock fire = (FireBlock) Blocks.FIRE;
          fire.setFlammable(CorkOakContent.LOG.get(), 5, 5);
          fire.setFlammable(CorkOakContent.LEAVES.get(), 30, 60);
          for (var block : java.util.List.of(PLANKS, STAIRS, SLAB, FENCE, FENCE_GATE))
            fire.setFlammable(block.get(), 5, 20);
        });
  }
}
