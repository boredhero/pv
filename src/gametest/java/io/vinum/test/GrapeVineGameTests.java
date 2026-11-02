package io.vinum.test;

import io.vinum.block.GrapeCropBlock;
import io.vinum.block.GrapeTrellisBlock;
import io.vinum.block.ModBlocks;
import io.vinum.crop.GrapeVariety;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual placement and picking interactions for perennial, two-cell vineyard rows. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class GrapeVineGameTests {
  private static final BlockPos ROOT = new BlockPos(4, 1, 4);

  static BlockHitResult hit(BlockPos pos, double x) {
    return new BlockHitResult(
        new Vec3(pos.getX() + x, pos.getY() + .5, pos.getZ() + .25), Direction.NORTH, pos, false);
  }

  static Player placeTrellis(GameTestHelper h, BlockPos root) {
    return placeTrellis(h, root, Direction.NORTH);
  }

  private static Player placeTrellis(GameTestHelper h, BlockPos root, Direction facing) {
    var level = h.getLevel();
    level.removeBlock(root, false);
    level.removeBlock(root.above(), false);
    level.setBlock(root.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setYRot(facing.getOpposite().toYRot());
    var item = new ItemStack(ModBlocks.GRAPE_TRELLIS.get());
    player.setItemInHand(InteractionHand.MAIN_HAND, item);
    var groundHit =
        new BlockHitResult(
            Vec3.atCenterOf(root.below()).add(0, .5, 0), Direction.UP, root.below(), false);
    h.assertTrue(
        item.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, item, groundHit))
            .consumesAction(),
        "Trellis item places on soil");
    h.assertTrue(
        level.getBlockState(root).is(ModBlocks.GRAPE_TRELLIS.get())
            && level.getBlockState(root.above()).is(ModBlocks.GRAPE_TRELLIS.get())
            && level.getBlockState(root).getValue(GrapeTrellisBlock.HALF) == DoubleBlockHalf.LOWER
            && level.getBlockState(root.above()).getValue(GrapeTrellisBlock.HALF)
                == DoubleBlockHalf.UPPER,
        "One crafted trellis reserves exactly two matching halves");
    h.assertTrue(item.isEmpty(), "Survival placement consumes exactly one trellis");
    return player;
  }

  private static Player plant(GameTestHelper h, BlockPos root) {
    return plant(h, root, Direction.NORTH);
  }

  private static Player plant(GameTestHelper h, BlockPos root, Direction facing) {
    var player = placeTrellis(h, root, facing);
    var seeds = GrapeVariety.MERLOT.seeds();
    player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
    h.assertTrue(
        seeds
            .useOn(
                new UseOnContext(
                    h.getLevel(), player, InteractionHand.MAIN_HAND, seeds, hit(root, .25)))
            .consumesAction(),
        "Seeds plant into lower trellis");
    h.assertTrue(seeds.isEmpty(), "Planting consumes one variety seed");
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    return player;
  }

  static int countDrops(GameTestHelper h, BlockPos root, Item item) {
    return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(root).inflate(2)).stream()
        .filter(e -> e.getItem().is(item))
        .mapToInt(e -> e.getItem().getCount())
        .sum();
  }

  @GameTest(template = "barrel_space")
  public static void blockedTrellisAndUnsupportedSeedsDoNotConsumeItems(GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    level.setBlock(root.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
    level.setBlock(root.above(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var trellis = new ItemStack(ModBlocks.GRAPE_TRELLIS.get(), 2);
    player.setItemInHand(InteractionHand.MAIN_HAND, trellis);
    var groundHit =
        new BlockHitResult(
            Vec3.atCenterOf(root.below()).add(0, .5, 0), Direction.UP, root.below(), false);
    trellis.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, trellis, groundHit));
    h.assertTrue(
        trellis.getCount() == 2
            && level.getBlockState(root).isAir()
            && level.getBlockState(root.above()).is(Blocks.STONE),
        "Blocked headroom leaves both the held trellis and obstruction untouched");
    level.removeBlock(root.above(), false);
    var seeds = GrapeVariety.MERLOT.seeds();
    player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
    seeds.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, seeds, groundHit));
    h.assertTrue(
        seeds.getCount() == 1 && level.getBlockState(root).isAir(),
        "Grape seeds require a trellis instead of planting a normal field crop");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void bunchesRipenArePickedIndividuallyAndRegrowOnSameVine(GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    var player = plant(h, root);
    var crop = ModBlocks.CROP_GRAPE_MERLOT.get();
    for (int age = 1; age <= 7; age++) {
      crop.performBonemeal(level, level.random, root.above(), level.getBlockState(root.above()));
      h.assertTrue(
          level.getBlockState(root).getValue(GrapeCropBlock.AGE) == age
              && level.getBlockState(root.above()).getValue(GrapeCropBlock.AGE) == age,
          "Bonemeal advances exactly one shared vine and ripening stage");
      if (age < 7) {
        level.getBlockState(root.above()).useWithoutItem(level, player, hit(root.above(), .25));
        h.assertTrue(
            countDrops(h, root, ModItems.GRAPES_MERLOT.get()) == 0,
            "Immature clusters cannot yield ripe grapes");
      }
    }
    level.getBlockState(root.above()).useWithoutItem(level, player, hit(root.above(), .25));
    int first = countDrops(h, root, ModItems.GRAPES_MERLOT.get());
    int remaining = level.getBlockState(root).getValue(GrapeCropBlock.BUNCHES);
    h.assertTrue(
        first >= 1
            && first <= 2
            && (remaining == 1 || remaining == 2)
            && level.getBlockState(root).getValue(GrapeCropBlock.AGE) == 7,
        "Picking one ripe cluster leaves its neighbor ripe and visible");
    var encoded =
        BlockState.CODEC
            .encodeStart(com.mojang.serialization.JsonOps.INSTANCE, level.getBlockState(root))
            .getOrThrow();
    h.assertTrue(
        BlockState.CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, encoded)
                .getOrThrow()
                .getValue(GrapeCropBlock.BUNCHES)
            == remaining,
        "Partly harvested clusters persist through blockstate serialization");
    level.getBlockState(root.above()).useWithoutItem(level, player, hit(root.above(), .25));
    h.assertTrue(
        countDrops(h, root, ModItems.GRAPES_MERLOT.get()) == first,
        "Clicking an already picked cluster cannot duplicate grapes");
    level.getBlockState(root).useWithoutItem(level, player, hit(root, .75));
    int total = countDrops(h, root, ModItems.GRAPES_MERLOT.get());
    h.assertTrue(
        total >= first + 1
            && total <= first + 2
            && level.getBlockState(root).is(crop)
            && level.getBlockState(root.above()).is(crop)
            && level.getBlockState(root).getValue(GrapeCropBlock.AGE) == 3
            && level.getBlockState(root.above()).getValue(GrapeCropBlock.AGE) == 3,
        "Picking the other cluster through the lower half restarts flowering without removing wood"
            + " or vine");
    for (int age = 4; age <= 7; age++)
      crop.performBonemeal(level, level.random, root, level.getBlockState(root));
    h.assertTrue(
        level.getBlockState(root).getValue(GrapeCropBlock.AGE) == 7
            && level.getBlockState(root).getValue(GrapeCropBlock.BUNCHES) == 3,
        "Established vines regrow both bunches without replanting");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void eitherHalfDismantlesOnceAndSoilLossLeavesNoFloatingVine(GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    for (boolean upper : new boolean[] {false, true}) {
      plant(h, root);
      int seeds = countDrops(h, root, ModItems.GRAPE_SEEDS.get());
      int trellises = countDrops(h, root, ModBlocks.GRAPE_TRELLIS.get().asItem());
      var miner = VinumGameTests.serverPlayer(h);
      miner.setGameMode(GameType.SURVIVAL);
      miner.moveTo(Vec3.atCenterOf(root));
      h.assertTrue(
          miner.gameMode.destroyBlock(upper ? root.above() : root), "Survival mining succeeds");
      h.assertTrue(
          level.getBlockState(root).isAir() && level.getBlockState(root.above()).isAir(),
          "Breaking either half cleans the entire supported vine");
      h.assertTrue(
          countDrops(h, root, ModItems.GRAPE_SEEDS.get()) == seeds + 1
              && countDrops(h, root, ModBlocks.GRAPE_TRELLIS.get().asItem()) == trellises + 1,
          "Dismantling a two-block vine returns one seed and one trellis, never two");
    }
    plant(h, root);
    level.removeBlock(root.below(), false);
    h.assertTrue(
        level.getBlockState(root).isAir() && level.getBlockState(root.above()).isAir(),
        "Losing soil cleans both vine halves immediately");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void rotatedRowsSelectIndependentBunchesAndCreativeRemovalDropsNothing(
      GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    var crop = ModBlocks.CROP_GRAPE_MERLOT.get();
    for (Direction facing : Direction.Plane.HORIZONTAL) {
      var player = plant(h, root, facing);
      h.assertTrue(
          level.getBlockState(root).getValue(GrapeCropBlock.FACING) == facing
              && level.getBlockState(root.above()).getValue(GrapeCropBlock.FACING) == facing,
          "Actual placement orients both vine halves: " + facing);
      crop.growTo(level, root, level.getBlockState(root), 7);
      var upper = root.above();
      double x = facing.getAxis() == Direction.Axis.Z ? .25 : .5;
      double z = facing.getAxis() == Direction.Axis.X ? .25 : .5;
      var firstHit =
          new BlockHitResult(
              new Vec3(upper.getX() + x, upper.getY() + .5, upper.getZ() + z),
              facing,
              upper,
              false);
      int before = countDrops(h, root, ModItems.GRAPES_MERLOT.get());
      level.getBlockState(upper).useWithoutItem(level, player, firstHit);
      int first = countDrops(h, root, ModItems.GRAPES_MERLOT.get());
      int remaining = level.getBlockState(root).getValue(GrapeCropBlock.BUNCHES);
      h.assertTrue(
          first > before && (remaining == 1 || remaining == 2),
          "Each orientation selects one cluster: " + facing);
      level.getBlockState(upper).useWithoutItem(level, player, firstHit);
      h.assertTrue(
          countDrops(h, root, ModItems.GRAPES_MERLOT.get()) == first,
          "Repeated picking is stable after rotation: " + facing);
      var secondHit =
          new BlockHitResult(
              new Vec3(upper.getX() + 1 - x, upper.getY() + .5, upper.getZ() + 1 - z),
              facing,
              upper,
              false);
      level.getBlockState(upper).useWithoutItem(level, player, secondHit);
      h.assertTrue(
          countDrops(h, root, ModItems.GRAPES_MERLOT.get()) > first
              && level.getBlockState(root).getValue(GrapeCropBlock.AGE) == 3,
          "Opposite cluster completes harvest in every orientation: " + facing);
    }
    plant(h, root);
    var creative = VinumGameTests.serverPlayer(h);
    creative.setGameMode(GameType.CREATIVE);
    creative.moveTo(Vec3.atCenterOf(root));
    int seeds = countDrops(h, root, ModItems.GRAPE_SEEDS.get());
    int trellises = countDrops(h, root, ModBlocks.GRAPE_TRELLIS.get().asItem());
    h.assertTrue(creative.gameMode.destroyBlock(root.above()), "Creative upper removal succeeds");
    h.assertTrue(
        level.getBlockState(root).isAir()
            && level.getBlockState(root.above()).isAir()
            && countDrops(h, root, ModItems.GRAPE_SEEDS.get()) == seeds
            && countDrops(h, root, ModBlocks.GRAPE_TRELLIS.get().asItem()) == trellises,
        "Creative removal cleans both halves without producing survival loot");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void wildGrapesProvideVarietySeedsWithoutFreeWoodenSupports(GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    level.setBlock(root.below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
    var wild =
        ModBlocks.CROP_GRAPE_MERLOT
            .get()
            .defaultBlockState()
            .setValue(GrapeCropBlock.AGE, 7)
            .setValue(GrapeCropBlock.TRELLISED, false);
    level.setBlock(root, wild, Block.UPDATE_ALL);
    var drops = Block.getDrops(wild, level, root, null);
    h.assertTrue(
        drops.stream()
                .anyMatch(
                    s ->
                        s.is(ModItems.GRAPE_SEEDS.get())
                            && io.vinum.item.GrapeSeedItem.variety(s).getPath().equals("merlot"))
            && drops.stream().anyMatch(s -> s.is(ModItems.GRAPES_MERLOT.get())),
        "Wild grapes remain a survival source of correctly typed seeds and fruit");
    h.assertTrue(
        drops.stream().noneMatch(s -> s.is(ModBlocks.GRAPE_TRELLIS.get().asItem()))
            && level.getBlockState(root.above()).isAir(),
        "Natural untrained vines cannot conjure crafted trellises or an unsupported upper half");
    h.succeed();
  }
}
