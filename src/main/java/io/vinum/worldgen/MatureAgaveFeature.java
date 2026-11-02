package io.vinum.worldgen;

import io.vinum.block.AgaveCropBlock;
import io.vinum.block.AgaveGeometry;
import io.vinum.block.AgaveLeafBlock;
import io.vinum.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Preflights the complete occupied footprint before writing through the generation region. */
public final class MatureAgaveFeature extends Feature<NoneFeatureConfiguration> {
  public MatureAgaveFeature() {
    super(NoneFeatureConfiguration.CODEC);
  }

  static boolean available(WorldGenLevel level, BlockPos pos) {
    return !level.isOutsideBuildHeight(pos)
        && level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getZ()))
        && level.getChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.EMPTY, false)
            != null
        && level.ensureCanWrite(pos);
  }

  @Override
  public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
    return placePlant(context.level(), context.origin(), 7);
  }

  static boolean placePlant(WorldGenLevel level, BlockPos root, int age) {
    var mature = ModBlocks.CROP_AGAVE.get().getStateForAge(age)
        .setValue(AgaveCropBlock.STRIPPED, false);
    if (!available(level, root.below())) return false;
    // Feature generation precedes lighting: CropBlock.canSurvive would consult uninitialized light.
    // Wild plants require the same sand/farmland substrate, without a runtime light check here.
    var soil = level.getBlockState(root.below());
    if (!soil.is(BlockTags.SAND) && !soil.is(Blocks.FARMLAND)) return false;
    var cells = AgaveGeometry.cells(age);
    for (BlockPos offset : cells) {
      BlockPos target = root.offset(offset);
      if (!available(level, target) || !level.getBlockState(target).isAir()) return false;
    }
    // Suppress neighbor notifications until the complete, owned plant exists.
    level.setBlock(root, mature, Block.UPDATE_CLIENTS);
    for (BlockPos offset : cells) {
      if (offset.equals(BlockPos.ZERO)) continue;
      level.setBlock(root.offset(offset), ModBlocks.AGAVE_LEAVES.get().defaultBlockState()
          .setValue(AgaveLeafBlock.X, offset.getX() + 1)
          .setValue(AgaveLeafBlock.Y, offset.getY())
          .setValue(AgaveLeafBlock.Z, offset.getZ() + 1)
          .setValue(AgaveCropBlock.AGE, age), Block.UPDATE_CLIENTS);
    }
    return true;
  }
}
