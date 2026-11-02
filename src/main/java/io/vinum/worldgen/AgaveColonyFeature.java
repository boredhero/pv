package io.vinum.worldgen;

import java.util.ArrayList;
import java.util.List;
import io.vinum.block.AgaveGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A mature mother plant surrounded by younger offsets, with room to grow on clear terrain. */
public final class AgaveColonyFeature extends Feature<NoneFeatureConfiguration> {
  private static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME,
      ResourceLocation.fromNamespaceAndPath("vinum", "has_agave"));

  public AgaveColonyFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
    var level = context.level();
    var root = context.origin();
    if (!MatureAgaveFeature.available(level, root) || !level.getBiome(root).is(BIOMES)
        || !MatureAgaveFeature.placePlant(level, root, 7)) return false;
    List<BlockPos> planted = new ArrayList<>();
    planted.add(root.immutable());
    double rotation = context.random().nextDouble() * Math.PI * 2;
    // Opposite inner neighbors and perpendicular outer pups avoid crowding the mother plant.
    for (int index = 0; index < 4; index++) {
      boolean inner = index < 2;
      int radius = inner ? 3 : 5;
      int age = inner ? 3 + context.random().nextInt(4) : context.random().nextInt(3);
      for (int attempt = 0; attempt < 4; attempt++) {
        double angle = rotation + (index % 2) * Math.PI + (inner ? 0 : Math.PI / 2)
            + (context.random().nextDouble() - 0.5) * 0.5;
        // A square ring guarantees three blocks of clearance even at diagonal angles.
        double scale = radius / Math.max(Math.abs(Math.cos(angle)), Math.abs(Math.sin(angle)));
        BlockPos column = root.offset((int) Math.round(Math.cos(angle) * scale), 0,
            (int) Math.round(Math.sin(angle) * scale));
        if (!MatureAgaveFeature.available(level, column)) continue;
        BlockPos target = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        if (!MatureAgaveFeature.available(level, target) || !level.getBiome(target).is(BIOMES)
            || planted.stream().anyMatch(p -> Math.max(Math.abs(p.getX() - target.getX()),
                Math.abs(p.getZ() - target.getZ())) < 3)
            || !clearMatureFootprint(level, target)) continue;
        if (MatureAgaveFeature.placePlant(level, target, age)) {
          planted.add(target.immutable());
          break;
        }
      }
    }
    return true;
  }

  private static boolean clearMatureFootprint(WorldGenLevel level, BlockPos root) {
    for (BlockPos offset : AgaveGeometry.cells(7)) {
      BlockPos target = root.offset(offset);
      if (!MatureAgaveFeature.available(level, target) || !level.getBlockState(target).isAir())
        return false;
    }
    return true;
  }
}
