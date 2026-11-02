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
  @GameTest(template = "barrel_space")
  public static void wildColonyHasMatureCenterAndYoungerSpacedOffsets(GameTestHelper h) {
    BlockPos center = h.absolutePos(new BlockPos(7, 1, 7));
    // Biome zoom can sample quart cells below/outside the structure at its floor.
    // Fill a padded volume so this surface test is independent of the world's random test origin.
    var biomeFill = net.minecraft.server.commands.FillBiomeCommand.fill(h.getLevel(),
        center.offset(-10, -8, -10), center.offset(10, 10, 10),
        h.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
            .getHolderOrThrow(net.minecraft.world.level.biome.Biomes.DESERT));
    h.assertTrue(biomeFill.left().isPresent(), "Padded desert fixture must initialize");
    // Remove the GameTest protective ceiling too: heightmap placement needs actual open sky.
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, 0, -6), center.offset(6, 16, 6)))
      h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -1, -6), center.offset(6, -1, 6)))
      h.getLevel().setBlock(pos, Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
    var colony = h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
        .get(ResourceLocation.fromNamespaceAndPath("vinum", "patch_agave"));
    h.assertTrue(colony.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(),
        net.minecraft.util.RandomSource.create(42), center), "Desert sand permits a colony; biome=" + h.getLevel().getBiome(center).unwrapKey()
        + ", soil=" + h.getLevel().getBlockState(center.below()) + ", root=" + h.getLevel().getBlockState(center));
    java.util.List<BlockPos> roots = new java.util.ArrayList<>();
    int middle = 0, outer = 0;
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, 0, -5), center.offset(5, 0, 5))) {
      var state = h.getLevel().getBlockState(pos);
      if (!state.is(ModBlocks.CROP_AGAVE.get())) continue;
      roots.add(pos.immutable());
      int distance = Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ()));
      int age = state.getValue(AgaveCropBlock.AGE);
      if (distance == 0) h.assertTrue(age == 7, "Colony mother is fully mature");
      else if (distance == 3) {
        h.assertTrue(age >= 3 && age <= 6, "Inner offsets are partly grown");
        middle++;
      } else {
        h.assertTrue(distance == 5 && age <= 2, "Outer offsets are seedlings");
        outer++;
      }
    }
    h.assertTrue(roots.size() == 5 && middle == 2 && outer == 2, "Clear terrain supports five age-graded plants: roots=" + roots + ", middle=" + middle + ", outer=" + outer
        + ", sample surface=" + h.getLevel().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, center.east(3))
        + ", center=" + center);
    for (BlockPos a : roots) for (BlockPos b : roots)
      if (!a.equals(b)) h.assertTrue(Math.max(Math.abs(a.getX() - b.getX()),
          Math.abs(a.getZ() - b.getZ())) >= 3, "Offsets have space to mature");
    // Reject an obstructed mother before placing any of its surrounding offspring.
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, 0, -6), center.offset(6, 16, 6)))
      h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    h.getLevel().setBlock(center, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
    h.assertTrue(!colony.place(h.getLevel(), h.getLevel().getChunkSource().getGenerator(),
        net.minecraft.util.RandomSource.create(42), center), "Obstructed mother rejects the colony");
    h.assertTrue(h.getLevel().getBlockState(center).is(Blocks.STONE), "Colony preserves existing blocks");
    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, 0, -6), center.offset(6, 3, 6)))
      if (!pos.equals(center)) h.assertTrue(h.getLevel().getBlockState(pos).isAir(),
          "Rejected colony leaves no partial offspring");
    h.succeed();
  }

}
