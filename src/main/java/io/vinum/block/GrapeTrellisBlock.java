package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.item.GrapeSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Two-cell vineyard support. Adjacent segments form continuous horizontal rows. */
public final class GrapeTrellisBlock extends Block {
  public static final MapCodec<GrapeTrellisBlock> CODEC = simpleCodec(GrapeTrellisBlock::new);
  public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

  public GrapeTrellisBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(FACING, Direction.NORTH));
  }

  @Override
  public MapCodec<GrapeTrellisBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(HALF, FACING);
  }

  public static boolean soil(BlockState state) {
    return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockPos pos = context.getClickedPos();
    if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 1
        || !soil(context.getLevel().getBlockState(pos.below()))
        || !context.getLevel().getBlockState(pos.above()).canBeReplaced(context)) return null;
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
    return context.getItemInHand().getItem() instanceof GrapeSeedItem;
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), UPDATE_ALL);
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    if (state.getValue(HALF) == DoubleBlockHalf.LOWER)
      return soil(level.getBlockState(pos.below()));
    BlockState lower = level.getBlockState(pos.below());
    return lower.is(this)
        && lower.getValue(HALF) == DoubleBlockHalf.LOWER
        && lower.getValue(FACING) == state.getValue(FACING);
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState neighbor,
      LevelAccessor level,
      BlockPos pos,
      BlockPos neighborPos) {
    return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return shape(state);
  }

  public static VoxelShape shape(BlockState state) {
    VoxelShape post = box(7, 0, 7, 9, 16, 9);
    if (state.getValue(HALF) == DoubleBlockHalf.LOWER)
      return state.getValue(FACING).getAxis() == Direction.Axis.Z
          ? Shapes.or(post, box(0, 15.7, 7.15, 16, 16, 7.5))
          : Shapes.or(post, box(7.15, 15.7, 0, 7.5, 16, 16));
    return state.getValue(FACING).getAxis() == Direction.Axis.Z
        ? Shapes.or(post, box(0, 11, 7, 16, 13, 9), box(0, 6, 7.15, 16, 6.3, 7.5))
        : Shapes.or(post, box(7, 11, 0, 9, 13, 16), box(7.15, 6, 0, 7.5, 6.3, 16));
  }

  @Override
  protected BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  protected BlockState mirror(BlockState state, Mirror mirror) {
    return rotate(state, mirror.getRotation(state.getValue(FACING)));
  }

  @Override
  public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    if (!level.isClientSide
        && player.hasInfiniteMaterials()
        && state.getValue(HALF) == DoubleBlockHalf.UPPER) level.removeBlock(pos.below(), false);
    return super.playerWillDestroy(level, pos, state, player);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    // Planting replaces the support with a vine which includes the same permanent timber.
    if (!replacement.is(this) && !(replacement.getBlock() instanceof GrapeCropBlock))
      removePartner(state, level, pos);
    super.onRemove(state, level, pos, replacement, moving);
  }

  public static void removePartner(BlockState state, Level level, BlockPos pos) {
    BlockPos other = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
    BlockState paired = level.getBlockState(other);
    if (paired.is(state.getBlock()) && paired.getValue(HALF) != state.getValue(HALF)) {
      if (state.getValue(HALF) == DoubleBlockHalf.UPPER) level.destroyBlock(other, true);
      else level.removeBlock(other, false);
    }
  }
}
