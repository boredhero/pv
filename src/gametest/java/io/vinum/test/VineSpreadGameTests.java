package io.vinum.test;

import io.vinum.block.GrapeCropBlock;
import io.vinum.block.ModBlocks;
import io.vinum.crop.GrapeVariety;
import io.vinum.crop.GrapeVineBlockEntity;
import io.vinum.crop.VineyardConfig;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class VineSpreadGameTests {
  private static BlockPos row(GameTestHelper h) {
    BlockPos root = h.absolutePos(new BlockPos(4, 1, 4));
    for (int x = -3; x <= 3; x++) GrapeVineGameTests.placeTrellis(h, root.offset(x, 0, 0));
    var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var seeds = GrapeVariety.MERLOT.seeds();
    player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
    h.assertTrue(
        seeds
            .useOn(
                new UseOnContext(
                    h.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND,
                    seeds,
                    GrapeVineGameTests.hit(root, .25)))
            .consumesAction(),
        "One seed plants the row origin");
    return root;
  }

  private static GrapeVineBlockEntity vine(GameTestHelper h, BlockPos pos) {
    return (GrapeVineBlockEntity) h.getLevel().getBlockEntity(pos);
  }

  private static int planted(GameTestHelper h, BlockPos root) {
    int count = 0;
    for (int x = -3; x <= 3; x++)
      if (h.getLevel().getBlockState(root.offset(x, 0, 0)).getBlock() instanceof GrapeCropBlock)
        count++;
    return count;
  }

  private static void fillRow(GameTestHelper h, BlockPos root) {
    var crop = (GrapeCropBlock) h.getLevel().getBlockState(root).getBlock();
    crop.growTo(h.getLevel(), root, h.getLevel().getBlockState(root), 7);
    RandomSource random = RandomSource.create(73);
    for (int n = 0; n < 256; n++) vine(h, root).naturalSpread(random);
  }

  @GameTest(template = "barrel_space")
  public static void naturalSpreadIsBoundedPersistentAndNeverTriggeredByBoneMeal(GameTestHelper h) {
    BlockPos root = row(h);
    var crop = (GrapeCropBlock) h.getLevel().getBlockState(root).getBlock();
    for (int n = 0; n < 100; n++)
      crop.performBonemeal(
          h.getLevel(), h.getLevel().random, root, h.getLevel().getBlockState(root));
    h.assertTrue(
        planted(h, root) == 1 && vine(h, root).sectionCount() == 1,
        "Bone meal ripens only the planted section and cannot spread the vine");
    fillRow(h, root);
    int expected = Math.min(VineyardConfig.maxTrellises(), 7);
    h.assertTrue(
        planted(h, root) == expected && vine(h, root).sectionCount() == expected,
        "Natural growth stops at the configured total including the origin");
    var saved = vine(h, root).saveWithoutMetadata(h.getLevel().registryAccess());
    vine(h, root).loadWithComponents(saved, h.getLevel().registryAccess());
    for (int x = -3; x <= 3; x++) {
      BlockPos pos = root.offset(x, 0, 0);
      if (x == 0 || !(h.getLevel().getBlockEntity(pos) instanceof GrapeVineBlockEntity branch))
        continue;
      var data = branch.saveWithoutMetadata(h.getLevel().registryAccess());
      branch.loadWithComponents(data, h.getLevel().registryAccess());
      h.assertTrue(
          branch.origin().equals(root) && branch.ownershipReady(),
          "Arms retain their original root and UUID through reload");
      h.assertTrue(
          !branch.naturalSpread(RandomSource.create(1)),
          "An arm cannot found another independently spreading vine");
      BlockState state = h.getLevel().getBlockState(pos);
      h.assertTrue(
          state.getValue(GrapeCropBlock.BRANCH)
              && h.getLevel().getBlockState(pos.above()).getValue(GrapeCropBlock.BRANCH),
          "Both arm halves use the connected-canopy model");
      h.assertTrue(
          Block.getDrops(state, h.getLevel(), pos, branch).stream()
              .noneMatch(s -> s.is(ModItems.GRAPE_SEEDS.get())),
          "Pruning immature arms cannot manufacture independent seedlings");
    }
    fillRow(h, root);
    h.assertTrue(
        planted(h, root) == expected, "Reloading ownership cannot reset the section limit");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void severingAnArmPrunesOnlyItsDisconnectedCanopy(GameTestHelper h) {
    BlockPos root = row(h);
    fillRow(h, root);
    int direction =
        h.getLevel().getBlockEntity(root.offset(2, 0, 0)) instanceof GrapeVineBlockEntity ? 1 : -1;
    h.assertTrue(
        h.getLevel().getBlockEntity(root.offset(direction * 2, 0, 0))
            instanceof GrapeVineBlockEntity,
        "The default five-section vine develops a connected arm at least two sections long");
    int opposite = 0;
    for (int n = 1; n <= 3; n++)
      if (h.getLevel().getBlockEntity(root.offset(-direction * n, 0, 0))
          instanceof GrapeVineBlockEntity) opposite++;
    h.getLevel().destroyBlock(root.offset(direction, 0, 0), true);
    for (int n = 2; n <= 3; n++)
      h.assertTrue(
          h.getLevel()
              .getBlockState(root.offset(direction * n, 0, 0))
              .is(ModBlocks.GRAPE_TRELLIS.get()),
          "Sections beyond a cut arm return to bare supports");
    h.assertTrue(
        planted(h, root) == 1 + opposite && vine(h, root).sectionCount() == 1 + opposite,
        "Pruning preserves the root and its opposite connected arm with an accurate ownership"
            + " count");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void uprootingRestoresSupportsAndOldArmsCannotAttachToReplacementRoots(
      GameTestHelper h) {
    BlockPos root = row(h);
    fillRow(h, root);
    BlockPos arm = null;
    for (int x = -3; x <= 3; x++)
      if (x != 0
          && h.getLevel().getBlockEntity(root.offset(x, 0, 0)) instanceof GrapeVineBlockEntity)
        arm = root.offset(x, 0, 0);
    h.assertTrue(arm != null, "The default row has at least one naturally grown arm");
    BlockState oldArm = h.getLevel().getBlockState(arm);
    var saved = vine(h, arm).saveWithoutMetadata(h.getLevel().registryAccess());
    h.getLevel().destroyBlock(root, true);
    h.assertTrue(
        planted(h, root) == 0, "Removing the root clears its connected canopy immediately");
    h.assertTrue(
        GrapeVineGameTests.countDrops(h, root, ModItems.GRAPE_SEEDS.get()) == 1
            && GrapeVineGameTests.countDrops(h, root, ModBlocks.GRAPE_TRELLIS.get().asItem()) == 1,
        "Uprooting drops only the removed root's seed and timber, without duplicating its arms");
    for (int x = -3; x <= 3; x++)
      if (x != 0)
        h.assertTrue(
            h.getLevel().getBlockState(root.offset(x, 0, 0)).is(ModBlocks.GRAPE_TRELLIS.get()),
            "Uprooting returns owned sections to their existing empty supports without dropping"
                + " extra timber");
    var crop = ModBlocks.CROP_GRAPE_MERLOT.get();
    BlockState fresh = crop.defaultBlockState().setValue(GrapeCropBlock.TRELLISED, true);
    h.getLevel().setBlock(root, fresh, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    h.getLevel()
        .setBlock(
            root.above(),
            fresh.setValue(GrapeCropBlock.HALF, DoubleBlockHalf.UPPER),
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    h.getLevel().setBlock(arm, oldArm, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    h.getLevel()
        .setBlock(
            arm.above(),
            oldArm.setValue(GrapeCropBlock.HALF, DoubleBlockHalf.UPPER),
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    vine(h, arm).loadWithComponents(saved, h.getLevel().registryAccess());
    h.assertTrue(
        !crop.isValidBonemealTarget(h.getLevel(), arm, oldArm),
        "Orphaned arms cannot consume bone meal");
    h.assertTrue(
        !vine(h, arm).validateBranch()
            && h.getLevel().getBlockState(arm).is(ModBlocks.GRAPE_TRELLIS.get()),
        "An unloaded old arm cannot attach to a new plant at the same coordinates");
    h.assertTrue(vine(h, root).sectionCount() == 1, "The replacement seedling owns only itself");
    h.succeed();
  }
}
