package io.vinum.test;

import io.vinum.block.AgaveCropBlock;
import io.vinum.block.AgaveGeometry;
import io.vinum.block.AgaveLeafBlock;
import io.vinum.block.ModBlocks;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class AgaveWorldgenGameTests {
  private static final BlockPos ROOT = new BlockPos(4, 1, 4);

  private static BlockPos prepare(GameTestHelper h) {
    BlockPos root = h.absolutePos(ROOT);
    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-1, 0, -1), root.offset(1, 2, 1)))
      h.getLevel().removeBlock(pos, false);
    h.setBlock(ROOT.below(), Blocks.SAND);
    return root;
  }

  private static boolean generate(GameTestHelper h, BlockPos root) {
    var level = h.getLevel();
    var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
        .get(ResourceLocation.fromNamespaceAndPath("vinum", "mature_agave"));
    h.assertTrue(feature != null, "The datapack must expose the mature wild agave feature");
    return feature.place(level, level.getChunkSource().getGenerator(), level.random, root);
  }

  @GameTest(template = "barrel_space")
  public static void wildAgaveIsCompleteAndRequiresLeafHarvest(GameTestHelper h) {
    BlockPos root = prepare(h);
    h.assertTrue(generate(h, root), "Wild agave generates on clear sand");
    var state = h.getLevel().getBlockState(root);
    h.assertTrue(state.is(ModBlocks.CROP_AGAVE.get())
        && state.getValue(AgaveCropBlock.AGE) == 7
        && !state.getValue(AgaveCropBlock.STRIPPED), "Wild piña starts mature but unstripped");
    for (BlockPos offset : AgaveGeometry.cells(7)) {
      if (offset.equals(BlockPos.ZERO)) continue;
      BlockPos pos = root.offset(offset);
      var leaf = h.getLevel().getBlockState(pos);
      h.assertTrue(AgaveLeafBlock.belongsTo(leaf, pos, root)
          && leaf.getValue(AgaveCropBlock.AGE) == 7,
          "Every mature leaf section is present and owned by its root");
    }
    h.assertTrue(Block.getDrops(state, h.getLevel(), root, null, null, new net.minecraft.world.item.ItemStack(ModItems.COA.get())).stream()
        .noneMatch(stack -> stack.is(ModItems.AGAVE.get())),
        "Natural generation cannot bypass stripping to yield piña");
    for (BlockPos offset : AgaveGeometry.cells(7))
      if (!offset.equals(BlockPos.ZERO)) h.getLevel().removeBlock(root.offset(offset), false);
    var stripped = h.getLevel().getBlockState(root);
    h.assertTrue(stripped.getValue(AgaveCropBlock.STRIPPED)
        && Block.getDrops(stripped, h.getLevel(), root, null, null, new net.minecraft.world.item.ItemStack(ModItems.COA.get())).stream()
            .anyMatch(stack -> stack.is(ModItems.AGAVE.get())),
        "Removing the wild leaves exposes the normal harvestable piña");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void obstructedWildAgaveDoesNotOverwriteOrLeavePartialPlant(GameTestHelper h) {
    BlockPos root = prepare(h);
    BlockPos obstruction = AgaveGeometry.cells(7).stream()
        .filter(offset -> offset.getY() == 2).findFirst().orElseThrow();
    h.getLevel().setBlock(root.offset(obstruction), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
    h.assertTrue(!generate(h, root), "An occupied upper leaf cell rejects the whole plant");
    for (BlockPos offset : AgaveGeometry.cells(7))
      h.assertTrue(h.getLevel().getBlockState(root.offset(offset))
          .is(offset.equals(obstruction) ? Blocks.STONE : Blocks.AIR),
          "Failed generation preserves the obstruction and places no partial plant");
    h.getLevel().removeBlock(root.offset(obstruction), false);
    h.setBlock(ROOT.below(), Blocks.STONE);
    h.assertTrue(!generate(h, root), "Wild plants require valid soil");
    h.assertTrue(h.getLevel().getBlockState(root).isAir(), "Invalid soil leaves no root");
    h.succeed();
  }
}
