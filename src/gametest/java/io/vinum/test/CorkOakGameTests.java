package io.vinum.test;

import io.vinum.item.ModItems;
import io.vinum.tree.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class CorkOakGameTests {
  private static final BlockPos ROOT = new BlockPos(7, 1, 7);

  private static CorkLogBlockEntity plant(GameTestHelper h) {
    h.setBlock(ROOT.below(), Blocks.DIRT);
    h.setBlock(ROOT, CorkOakContent.LOG.get());
    h.setBlock(
        ROOT.above(),
        CorkOakContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    return (CorkLogBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(ROOT));
  }

  @GameTest(template = "barrel_space")
  public static void axePeelsOnceWithoutFelling(GameTestHelper h) {
    var bark = plant(h);
    var level = h.getLevel();
    var pos = h.absolutePos(ROOT);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var axe = new ItemStack(Items.IRON_AXE);
    player.setItemInHand(InteractionHand.MAIN_HAND, axe);
    var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
    level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
    h.assertTrue(
        bark.harvested() && level.getBlockState(pos).getValue(CorkLogBlock.HARVESTED),
        "Axe leaves the same trunk with exposed used bark");
    int drops =
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(e -> e.getItem().is(ModItems.CORK_BARK.get()))
            .mapToInt(e -> e.getItem().getCount())
            .sum();
    h.assertTrue(
        drops >= 2 && drops <= 3 && axe.getDamageValue() == 1,
        "Harvest yields 2-3 bark and wears axe once");
    level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
    int after =
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
            .filter(e -> e.getItem().is(ModItems.CORK_BARK.get()))
            .mapToInt(e -> e.getItem().getCount())
            .sum();
    h.assertTrue(
        after == drops && axe.getDamageValue() == 1,
        "Used trunk cannot duplicate cork or consume durability");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void harvestedLogPersistsDeadlineInLootAndReload(GameTestHelper h) {
    var bark = plant(h);
    bark.harvest();
    long deadline = bark.readyAt();
    var level = h.getLevel();
    var pos = h.absolutePos(ROOT);
    var saved = bark.saveWithFullMetadata(level.registryAccess());
    var restored = new CorkLogBlockEntity(pos, level.getBlockState(pos));
    restored.loadWithComponents(saved, level.registryAccess());
    h.assertTrue(restored.readyAt() == deadline, "Chunk reload preserves exact harvest deadline");
    var drops = Block.getDrops(level.getBlockState(pos), level, pos, bark);
    h.assertTrue(
        drops.size() == 1 && drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA),
        "Breaking used log preserves data in its item");
    var itemTag = drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
    h.assertTrue(
        itemTag.getLong("CorkReadyAt") == deadline && drops.getFirst().getMaxStackSize() == 1,
        "Used logs carry deadline and cannot merge distinct histories");
    h.assertTrue(
        new ItemStack(CorkOakContent.LOG_ITEM.get()).getMaxStackSize() == 64,
        "Fresh lumber remains stackable");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void regrowthHonorsDeadlineAndLivingTree(GameTestHelper h) {
    var bark = plant(h);
    var level = h.getLevel();
    var tag = new CompoundTag();
    long now = level.getGameTime();
    tag.putLong("CorkReadyAt", now + 20);
    bark.loadWithComponents(tag, level.registryAccess());
    h.assertTrue(
        !bark.refresh() && bark.nextCheckDelay(now) == 20, "Bark cannot regrow before deadline");
    tag.putLong("CorkReadyAt", now);
    bark.loadWithComponents(tag, level.registryAccess());
    h.setBlock(ROOT.above(), Blocks.AIR);
    h.assertTrue(
        !bark.refresh() && bark.nextCheckDelay(now) == 1200,
        "Detached or leafless wood waits without polling every tick");
    h.setBlock(ROOT.above(), CorkOakContent.LEAVES.get());
    h.assertTrue(
        bark.refresh() && !bark.harvested(), "Living rooted tree renews exactly at deadline");
    h.assertTrue(CorkOakConfig.regrowthTicks() >= 20, "Configured interval respects minimum bound");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void blockedTreePlacementIsAtomic(GameTestHelper h) {
    h.setBlock(ROOT.below(), Blocks.DIRT);
    h.setBlock(ROOT.above(2), Blocks.STONE);
    boolean placed =
        CorkOakFeature.placeTree(
            h.getLevel(),
            RandomSource.create(42),
            h.absolutePos(ROOT),
            new CorkOakFeature.Configuration(false, 8, 10));
    h.assertTrue(
        !placed && h.getBlockState(ROOT).isAir() && h.getBlockState(ROOT.above(2)).is(Blocks.STONE),
        "Obstructed tree preserves builds and places no partial trunk");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void saplingGrowsBroadForkedTreeThroughConfiguredFeature(GameTestHelper h) {
    var level = h.getLevel();
    var root = h.absolutePos(ROOT);
    for (BlockPos p : BlockPos.betweenClosed(root.offset(-7, 0, -7), root.offset(7, 12, 7)))
      level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
    h.setBlock(ROOT.below(), Blocks.DIRT);
    h.setBlock(
        ROOT, CorkOakContent.SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1));
    CorkOakContent.SAPLING
        .get()
        .performBonemeal(level, RandomSource.create(42), root, level.getBlockState(root));
    h.assertTrue(
        level.getBlockState(root).is(CorkOakContent.LOG.get()),
        "Bonemeal uses registered cork oak feature");
    int logs = 0,
        leaves = 0,
        horizontal = 0,
        minX = 999,
        minZ = 999,
        maxX = -999,
        maxZ = -999,
        maxY = 0;
    for (BlockPos p : BlockPos.betweenClosed(root.offset(-7, 0, -7), root.offset(7, 12, 7))) {
      var s = level.getBlockState(p);
      if (s.is(CorkOakContent.LOG.get())) {
        logs++;
        if (s.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) horizontal++;
      }
      if (s.is(CorkOakContent.LEAVES.get())) {
        leaves++;
        minX = Math.min(minX, p.getX() - root.getX());
        maxX = Math.max(maxX, p.getX() - root.getX());
        minZ = Math.min(minZ, p.getZ() - root.getZ());
        maxZ = Math.max(maxZ, p.getZ() - root.getZ());
        maxY = Math.max(maxY, p.getY() - root.getY());
      }
    }
    h.assertTrue(
        logs > 20 && horizontal > 4 && leaves > 100,
        "Mature tree has multiple spreading branches and a substantial evergreen crown");
    h.assertTrue(
        maxX - minX + 1 <= 14 && maxZ - minZ + 1 <= 14 && maxY + 1 <= 11,
        "Measured canopy stays within 14 blocks wide and 11 blocks tall");
    h.assertTrue(
        level
            .registryAccess()
            .registryOrThrow(Registries.PLACED_FEATURE)
            .containsKey(CorkOakContent.id("cork_oak_grove")),
        "Rare grove placed feature loads");
    h.succeed();
  }
}
