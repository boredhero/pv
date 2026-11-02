package io.vinum.test;

import io.vinum.block.*;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class AgaveGrowthGameTests {
  private static final BlockPos ROOT = new BlockPos(4, 1, 4);

  private static AgaveCropBlock crop() {
    return ModBlocks.CROP_AGAVE.get();
  }

  private static BlockPos plant(GameTestHelper h, int age) {
    BlockPos root = h.absolutePos(ROOT);
    for (BlockPos pos : BlockPos.betweenClosed(root.offset(-1, 0, -1), root.offset(1, 2, 1)))
      h.getLevel().removeBlock(pos, false);
    h.setBlock(ROOT.below(), Blocks.SAND);
    h.setBlock(ROOT, crop().getStateForAge(age));
    return root;
  }

  @GameTest(template = "barrel_space")
  public static void agaveHasEightVisibleStagesAndSparseMatureFootprint(GameTestHelper h) {
    BlockPos root = plant(h, 0);
    for (int age = 1; age <= 7; age++) {
      crop()
          .performBonemeal(
              h.getLevel(), h.getLevel().random, root, h.getLevel().getBlockState(root));
      h.assertTrue(
          h.getLevel().getBlockState(root).getValue(AgaveCropBlock.AGE) == age,
          "Each bonemeal advances exactly one agave stage");
      for (BlockPos offset : AgaveGeometry.cells(age)) {
        if (offset.equals(BlockPos.ZERO)) continue;
        var state = h.getLevel().getBlockState(root.offset(offset));
        h.assertTrue(
            AgaveLeafBlock.belongsTo(state, root.offset(offset), root)
                && state.getValue(AgaveCropBlock.AGE) == age,
            "Every rendered agave cell must reserve the matching growth stage");
      }
    }
    var cells = AgaveGeometry.cells(7);
    h.assertTrue(
        cells.stream().anyMatch(p -> p.getX() == -1)
            && cells.stream().anyMatch(p -> p.getX() == 1)
            && cells.stream().anyMatch(p -> p.getZ() == -1)
            && cells.stream().anyMatch(p -> p.getZ() == 1)
            && cells.stream().anyMatch(p -> p.getY() == 2),
        "Mature blue agave spans3x3x3");
    h.assertTrue(cells.size() < 27, "Empty corners are not filled with invisible crop blocks");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void obstructedAgaveGrowthDoesNotOverwriteOrPartiallyExpand(GameTestHelper h) {
    BlockPos root = plant(h, 3);
    BlockPos offset =
        AgaveGeometry.cells(4).stream()
            .filter(p -> !p.equals(BlockPos.ZERO))
            .findFirst()
            .orElseThrow();
    BlockPos obstacle = root.offset(offset);
    h.getLevel().setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
    h.assertTrue(
        !crop().isValidBonemealTarget(h.getLevel(), root, h.getLevel().getBlockState(root)),
        "Blocked growth must not consume bonemeal");
    h.assertTrue(
        !crop().growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 4),
        "A blocked stage cannot expand");
    h.assertTrue(
        h.getLevel().getBlockState(root).getValue(AgaveCropBlock.AGE) == 3
            && h.getLevel().getBlockState(obstacle).is(Blocks.STONE),
        "Root and obstruction remain unchanged");
    for (BlockPos cell : AgaveGeometry.cells(4))
      if (!cell.equals(BlockPos.ZERO) && !cell.equals(offset))
        h.assertTrue(
            h.getLevel().getBlockState(root.offset(cell)).isAir(),
            "Failed expansion has no partial leaf placement");
    h.getLevel().removeBlock(obstacle, false);
    h.assertTrue(
        crop().growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 4),
        "Removing the obstacle permits normal growth");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void cutLeavesBeforeHarvestingExposedPinaExactlyOnce(GameTestHelper h) {
    BlockPos root = plant(h, 0);
    h.assertTrue(
        crop().growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 7),
        "Mature setup must succeed");
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var mature = h.getLevel().getBlockState(root);
    h.assertTrue(
        mature.getDestroyProgress(player, h.getLevel(), root) == 0,
        "Unstripped piña cannot be mined normally");
    h.assertTrue(
        Block.getDrops(mature, h.getLevel(), root, null).stream()
            .noneMatch(stack -> stack.is(ModItems.AGAVE.get())),
        "Even forced removal cannot bypass leaf stripping to produce piña");
    var leaves = AgaveGeometry.cells(7).stream().filter(p -> !p.equals(BlockPos.ZERO)).toList();
    BlockPos first = root.offset(leaves.getFirst());
    var axe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
    var cut =
        h.getLevel()
            .getBlockState(first)
            .useItemOn(
                axe,
                h.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(first),
                    net.minecraft.core.Direction.UP,
                    first,
                    false));
    h.assertTrue(
        cut == net.minecraft.world.ItemInteractionResult.CONSUME
            && axe.getDamageValue() == 1
            && h.getLevel().getBlockState(first).isAir(),
        "Axe right-click cuts one leaf cell and uses one durability");
    h.assertTrue(
        h.getLevel().getBlockState(root).is(crop())
            && !h.getLevel().getBlockState(root).getValue(AgaveCropBlock.STRIPPED),
        "One cut cannot harvest or prematurely expose the heart");
    for (int tick = 0; tick < 20; tick++)
      h.getLevel().getBlockState(root).randomTick(h.getLevel(), root, h.getLevel().random);
    h.assertTrue(
        h.getLevel().getBlockState(first).isAir(),
        "Mature cut leaves do not regrow on random ticks");
    for (BlockPos offset : leaves)
      if (!offset.equals(leaves.getFirst())) h.getLevel().destroyBlock(root.offset(offset), true);
    h.assertTrue(
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(3)).isEmpty(),
        "Cut leaves yield no piña or duplicate seeds");
    var exposed = h.getLevel().getBlockState(root);
    h.assertTrue(
        exposed.getValue(AgaveCropBlock.STRIPPED)
            && exposed.getDestroyProgress(player, h.getLevel(), root) > 0,
        "The final cut exposes a harvestable piña");
    var saved =
        net.minecraft.world.level.block.state.BlockState.CODEC
            .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, exposed)
            .getOrThrow();
    h.assertTrue(
        net.minecraft.world.level.block.state.BlockState.CODEC
            .parse(com.mojang.serialization.JsonOps.INSTANCE, saved)
            .getOrThrow()
            .getValue(AgaveCropBlock.STRIPPED),
        "Exposed-heart state survives block-state serialization");
    h.getLevel().destroyBlock(root, true);
    var drops =
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(3)).stream()
            .map(ItemEntity::getItem)
            .toList();
    h.assertTrue(
        drops.stream()
                .filter(stack -> stack.is(ModItems.AGAVE_SEEDS.get()))
                .mapToInt(stack -> stack.getCount())
                .sum()
            == 1,
        "The harvested root yields seeds exactly once");
    h.assertTrue(
        drops.stream()
                .filter(stack -> stack.is(ModItems.AGAVE.get()))
                .mapToInt(stack -> stack.getCount())
                .sum()
            >= 1,
        "The exposed heart yields the existing agave processing ingredient");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void creativeLeafRemovalProducesNoHarvestAndSupportLossCleansUp(GameTestHelper h) {
    BlockPos root = plant(h, 0);
    crop().growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 7);
    BlockPos leaf =
        root.offset(
            AgaveGeometry.cells(7).stream()
                .filter(p -> !p.equals(BlockPos.ZERO))
                .findFirst()
                .orElseThrow());
    var creative = h.makeMockPlayer(GameType.CREATIVE);
    creative.getAbilities().instabuild = true;
    var state = h.getLevel().getBlockState(leaf);
    state.getBlock().playerWillDestroy(h.getLevel(), leaf, state, creative);
    h.getLevel().removeBlock(leaf, false);
    h.assertTrue(
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(3)).isEmpty(),
        "Creative leaf removal cannot duplicate produce");
    plant(h, 0);
    crop().growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 7);
    h.getLevel().removeBlock(root.below(), false);
    for (BlockPos cell : AgaveGeometry.cells(7))
      h.assertTrue(
          h.getLevel().getBlockState(root.offset(cell)).isAir(),
          "Losing the root substrate cleans all leaf cells");
    h.succeed();
  }
}
