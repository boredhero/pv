package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Eight-stage blue agave. The root owns harvesting; leaf cells reserve its actual growing space.
 */
public final class AgaveCropBlock extends CropBlock {
  public static final MapCodec<AgaveCropBlock> CODEC = simpleCodec(AgaveCropBlock::new);
  public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
  public static final BooleanProperty STRIPPED = BooleanProperty.create("stripped");

  public AgaveCropBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(AGE, 0).setValue(STRIPPED, false));
  }

  @Override
  public MapCodec<AgaveCropBlock> codec() {
    return CODEC;
  }

  @Override
  public IntegerProperty getAgeProperty() {
    return AGE;
  }

  @Override
  public int getMaxAge() {
    return 7;
  }

  @Override
  protected ItemLike getBaseSeedId() {
    return ModItems.AGAVE_SEEDS.get();
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AGE, STRIPPED);
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
    return state.is(BlockTags.SAND) || super.mayPlaceOn(state, level, pos);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return state.getValue(STRIPPED)
        ? Block.box(2, 0, 2, 14, 15, 14)
        : AgaveGeometry.shape(getAge(state), 0, 0, 0);
  }

  @Override
  protected int getBonemealAgeIncrease(Level level) {
    return 1;
  }

  @Override
  public void performBonemeal(
      ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    growTo(level, pos, state, Math.min(getAge(state) + 1, getMaxAge()));
  }

  @Override
  protected void randomTick(
      BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (isMaxAge(state)) {
      AgaveHarvest.removeDetachedLeaves(level, pos);
      return;
    }
    if (level.getRawBrightness(pos, 0) >= 9 && random.nextInt(7) == 0)
      growTo(level, pos, state, getAge(state) + 1);
  }

  /**
   * Checks the complete next footprint before mutating any block; growth never replaces existing
   * plants or builds.
   */
  @Override
  public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
    return !isMaxAge(state) && canGrowTo(level, pos, state, getAge(state) + 1);
  }

  private boolean canGrowTo(LevelReader level, BlockPos root, BlockState state, int age) {
    if (!state.is(this) || age <= getAge(state) || age > getMaxAge()) return false;
    for (BlockPos offset : AgaveGeometry.cells(age)) {
      BlockPos target = root.offset(offset);
      if (target.getY() >= level.getMaxBuildHeight()
          || target.getY() < level.getMinBuildHeight()
          || level.getChunk(
                  SectionPos.blockToSectionCoord(target.getX()),
                  SectionPos.blockToSectionCoord(target.getZ()),
                  net.minecraft.world.level.chunk.status.ChunkStatus.FULL,
                  false)
              == null) return false;
      if (offset.equals(BlockPos.ZERO)) continue;
      BlockState occupied = level.getBlockState(target);
      if (!occupied.isAir() && !AgaveLeafBlock.belongsTo(occupied, target, root)) return false;
    }
    return true;
  }

  public boolean growTo(ServerLevel level, BlockPos root, BlockState state, int age) {
    if (!canGrowTo(level, root, state, age)) return false;
    // Client-only notifications until every part is present prevent transient neighbor orphaning.
    level.setBlock(root, getStateForAge(age), Block.UPDATE_CLIENTS);
    for (BlockPos offset : AgaveGeometry.cells(age)) {
      if (offset.equals(BlockPos.ZERO)) continue;
      level.setBlock(
          root.offset(offset),
          ModBlocks.AGAVE_LEAVES
              .get()
              .defaultBlockState()
              .setValue(AgaveLeafBlock.X, offset.getX() + 1)
              .setValue(AgaveLeafBlock.Y, offset.getY())
              .setValue(AgaveLeafBlock.Z, offset.getZ() + 1)
              .setValue(AGE, age),
          Block.UPDATE_CLIENTS);
    }
    // Any old geometry cell absent from the new footprint is detached only after updating the root.
    for (BlockPos offset : AgaveGeometry.cells(getAge(state))) {
      if (offset.equals(BlockPos.ZERO) || AgaveGeometry.cells(age).contains(offset)) continue;
      BlockPos target = root.offset(offset);
      if (AgaveLeafBlock.belongsTo(level.getBlockState(target), target, root))
        level.removeBlock(target, false);
    }
    level.updateNeighborsAt(root, this);
    return true;
  }

  /**
   * -1 means a required neighboring chunk is unavailable; never mistake unloading for a cut leaf.
   */
  public static int remainingLeaves(Level level, BlockPos root) {
    BlockState state = level.getBlockState(root);
    if (!(state.getBlock() instanceof AgaveCropBlock) || state.getValue(AGE) != 7) return 0;
    int remaining = 0;
    for (BlockPos offset : AgaveGeometry.cells(7)) {
      if (offset.equals(BlockPos.ZERO)) continue;
      BlockPos pos = root.offset(offset);
      if (!level.hasChunk(
          SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
        return -1;
      if (AgaveLeafBlock.belongsTo(level.getBlockState(pos), pos, root)) remaining++;
    }
    return remaining;
  }

  public static void refreshStripped(Level level, BlockPos root) {
    if (level.isClientSide) return;
    BlockState state = level.getBlockState(root);
    if (state.getBlock() instanceof AgaveCropBlock
        && state.getValue(AGE) == 7
        && !state.getValue(STRIPPED)
        && remainingLeaves(level, root) == 0)
      level.setBlock(root, state.setValue(STRIPPED, true), Block.UPDATE_ALL);
  }

  @Override
  public float getDestroyProgress(
      BlockState state,
      net.minecraft.world.entity.player.Player player,
      BlockGetter level,
      BlockPos pos) {
    if (!player.hasInfiniteMaterials()
        && (!player.getMainHandItem().is(ModItems.COA.get())
            || (isMaxAge(state) && !state.getValue(STRIPPED)))) return 0;
    return super.getDestroyProgress(state, player, level, pos);
  }

  @Override
  protected net.minecraft.world.InteractionResult useWithoutItem(
      BlockState state,
      Level level,
      BlockPos pos,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.phys.BlockHitResult hit) {
    if (isMaxAge(state)) {
      if (!level.isClientSide)
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable(
                state.getValue(STRIPPED)
                    ? "block.vinum.agave_pina_ready"
                    : "block.vinum.agave_cut_leaves",
                remainingLeaves(level, pos)),
            true);
      return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    return super.useWithoutItem(state, level, pos, player, hit);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!replacement.is(this)) {
      for (BlockPos offset : AgaveGeometry.cells(getAge(state))) {
        if (offset.equals(BlockPos.ZERO)) continue;
        BlockPos target = pos.offset(offset);
        if (level.hasChunk(
                SectionPos.blockToSectionCoord(target.getX()),
                SectionPos.blockToSectionCoord(target.getZ()))
            && AgaveLeafBlock.belongsTo(level.getBlockState(target), target, pos))
          level.removeBlock(target, false);
      }
    }
    super.onRemove(state, level, pos, replacement, moving);
  }
}
