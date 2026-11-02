package io.vinum.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Bounded, preflighted low-forking oak with an asymmetric broad evergreen crown. */
public final class CorkOakFeature extends Feature<CorkOakFeature.Configuration> {
  public record Configuration(boolean grove, int minHeight, int maxHeight)
      implements FeatureConfiguration {
    public static final Codec<Configuration> CODEC =
        RecordCodecBuilder.create(
            i ->
                i.group(
                        Codec.BOOL.optionalFieldOf("grove", false).forGetter(Configuration::grove),
                        Codec.intRange(6, 12)
                            .optionalFieldOf("min_height", 8)
                            .forGetter(Configuration::minHeight),
                        Codec.intRange(6, 12)
                            .optionalFieldOf("max_height", 10)
                            .forGetter(Configuration::maxHeight))
                    .apply(i, Configuration::new));

    public Configuration {
      if (maxHeight < minHeight)
        throw new IllegalArgumentException("Cork-oak height range is reversed");
    }
  }

  public CorkOakFeature() {
    super(Configuration.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<Configuration> context) {
    var level = context.level();
    var random = context.random();
    var origin = context.origin();
    var config = context.config();
    boolean placed = placeTree(level, random, origin, config);
    if (!config.grove()) return placed;
    int desired = 1 + random.nextInt(2), added = 0;
    for (int attempt = 0; attempt < 6 && added < desired; attempt++) {
      int dx = random.nextInt(21) - 10, dz = random.nextInt(21) - 10;
      if (dx * dx + dz * dz < 49) continue;
      BlockPos target = origin.offset(dx, 0, dz);
      if (!loaded(level, target)) continue;
      target = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, target);
      if (placeTree(level, random, target, config)) {
        placed = true;
        added++;
      }
    }
    return placed;
  }

  private static boolean loaded(WorldGenLevel level, BlockPos pos) {
    return level.getChunk(
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getZ()),
            ChunkStatus.EMPTY,
            false)
        != null;
  }

  private static boolean replaceable(BlockState state) {
    return state.isAir()
        || state.is(CorkOakContent.SAPLING.get())
        || (state.is(BlockTags.REPLACEABLE_BY_TREES)
            && !(state.getBlock() instanceof LeavesBlock
                && state.getValue(LeavesBlock.PERSISTENT)));
  }

  public static boolean placeTree(
      WorldGenLevel level, RandomSource random, BlockPos origin, Configuration config) {
    if (!loaded(level, origin.below())
        || !(level.getBlockState(origin.below()).is(BlockTags.DIRT)
            || level.getBlockState(origin.below()).is(Blocks.FARMLAND))) return false;
    int height = config.minHeight() + random.nextInt(config.maxHeight() - config.minHeight() + 1);
    Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
    Set<BlockPos> leaves = new LinkedHashSet<>();
    BlockPos tip = origin;
    Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
    // A short stout trunk forks below its crown rather than becoming a narrow vertical pole.
    for (int y = 0; y < height - 2; y++) {
      BlockPos next = origin.above(y);
      if (y >= 4) next = next.relative(lean);
      if (y == 4) logs.put(origin.above(y), log(lean.getAxis()));
      logs.put(next, log(Direction.Axis.Y));
      tip = next;
    }
    crown(leaves, tip.above(), 3, 2, random);
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      int startHeight = 3 + random.nextInt(2);
      BlockPos branch = origin.above(startHeight);
      if (startHeight >= 4) branch = branch.relative(lean);
      int reach = 3 + random.nextInt(2);
      for (int step = 0; step < reach; step++) {
        branch = branch.relative(direction);
        logs.put(branch, log(direction.getAxis()));
        if (step % 2 == 0) {
          branch = branch.above();
          logs.put(branch, log(Direction.Axis.Y));
        }
      }
      crown(leaves, branch.above(), Math.min(6 - reach, 2 + random.nextInt(2)), 2, random);
    }
    leaves.removeAll(logs.keySet());
    // Validate the complete footprint first: never cut through a build or generate half a tree.
    Set<BlockPos> footprint = new HashSet<>(leaves);
    footprint.addAll(logs.keySet());
    for (BlockPos pos : footprint) {
      if (pos.getY() < level.getMinBuildHeight()
          || pos.getY() >= level.getMaxBuildHeight()
          || !loaded(level, pos)
          || !level.ensureCanWrite(pos)
          || !replaceable(level.getBlockState(pos))) return false;
    }
    logs.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
    // Distances are derived from actual branches so outer foliage decays normally when felled.
    for (BlockPos pos : leaves) {
      int distance = 7;
      for (BlockPos log : logs.keySet()) distance = Math.min(distance, pos.distManhattan(log));
      if (distance < 7)
        level.setBlock(
            pos,
            CorkOakContent.LEAVES
                .get()
                .defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, Math.max(1, distance)),
            Block.UPDATE_CLIENTS);
    }
    return true;
  }

  private static BlockState log(Direction.Axis axis) {
    return CorkOakContent.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
  }

  private static void crown(
      Set<BlockPos> leaves, BlockPos center, int radius, int height, RandomSource random) {
    for (int y = -1; y <= height; y++)
      for (int x = -radius; x <= radius; x++)
        for (int z = -radius; z <= radius; z++) {
          double d =
              (x * x + z * z) / (double) (radius * radius)
                  + (y * y) / (double) ((height + 1) * (height + 1));
          if (d > 1.3 || (d > 1 && random.nextInt(4) == 0)) continue;
          leaves.add(center.offset(x, y, z));
        }
  }
}
