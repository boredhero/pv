package io.vinum.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.crop.GrapeVariety;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Permanent trained vines; fruit ripens independently of harvesting the woody plant. */
public final class GrapeCropBlock extends CropBlock {
  public static final MapCodec<GrapeCropBlock> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      GrapeVariety.CODEC.fieldOf("variety").forGetter(GrapeCropBlock::getVariety),
                      propertiesCodec())
                  .apply(instance, (v, p) -> new GrapeCropBlock(p, v)));
  public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
  public static final EnumProperty<DoubleBlockHalf> HALF = GrapeTrellisBlock.HALF;
  public static final DirectionProperty FACING = GrapeTrellisBlock.FACING;
  public static final BooleanProperty TRELLISED = BooleanProperty.create("trellised");

  /** Bits 1 and 2 represent the two hanging clusters, seen from the model's front. */
  public static final IntegerProperty BUNCHES = IntegerProperty.create("bunches", 0, 3);

  public static final int REGROWTH_AGE = 3;
  private final GrapeVariety variety;

  public GrapeCropBlock(Properties properties, GrapeVariety variety) {
    super(properties);
    this.variety = variety;
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(AGE, 0)
            .setValue(HALF, DoubleBlockHalf.LOWER)
            .setValue(FACING, Direction.NORTH)
            .setValue(TRELLISED, false)
            .setValue(BUNCHES, 3));
  }

  @Override
  public MapCodec<GrapeCropBlock> codec() {
    return CODEC;
  }

  public GrapeVariety getVariety() {
    return variety;
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
    return variety.seedItem();
  }

  @Override
  public ItemStack getCloneItemStack(
      BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
    return variety.seeds();
  }

  @Override
  protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
    return GrapeTrellisBlock.soil(state);
  }

  public static BlockPos rootPos(BlockState state, BlockPos pos) {
    return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
  }

  @Override
  protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
    if (state.getValue(HALF) == DoubleBlockHalf.LOWER)
      return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    BlockState lower = level.getBlockState(pos.below());
    return state.getValue(TRELLISED)
        && lower.is(this)
        && lower.getValue(HALF) == DoubleBlockHalf.LOWER
        && lower.getValue(TRELLISED)
        && lower.getValue(FACING) == state.getValue(FACING);
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    if (state.getValue(TRELLISED))
      level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), UPDATE_ALL);
  }

  @Override
  public boolean isRandomlyTicking(BlockState state) {
    return state.getValue(HALF) == DoubleBlockHalf.LOWER && !isMaxAge(state);
  }

  @Override
  protected void randomTick(
      BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(7) == 0)
      growTo(level, pos, state, getAge(state) + 1);
  }

  @Override
  public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
    BlockState root = level.getBlockState(rootPos(state, pos));
    return root.is(this) && !isMaxAge(root);
  }

  @Override
  public void performBonemeal(
      ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
    pos = rootPos(state, pos);
    state = level.getBlockState(pos);
    if (state.is(this)) growTo(level, pos, state, Math.min(7, getAge(state) + 1));
  }

  public boolean growTo(ServerLevel level, BlockPos pos, BlockState state, int age) {
    pos = rootPos(state, pos);
    state = level.getBlockState(pos);
    if (!state.is(this) || age <= getAge(state) || age > 7 || !complete(level, pos, state))
      return false;
    updatePair(level, pos, state.setValue(AGE, age));
    return true;
  }

  private boolean complete(LevelReader level, BlockPos pos, BlockState state) {
    if (!state.getValue(TRELLISED)) return true;
    BlockState upper = level.getBlockState(pos.above());
    return upper.is(this)
        && upper.getValue(HALF) == DoubleBlockHalf.UPPER
        && upper.getValue(TRELLISED)
        && upper.getValue(FACING) == state.getValue(FACING);
  }

  private void updatePair(Level level, BlockPos pos, BlockState state) {
    level.setBlock(pos, state, UPDATE_CLIENTS);
    if (state.getValue(TRELLISED))
      level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), UPDATE_CLIENTS);
    level.updateNeighborsAt(pos, this);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return harvest(level, pos, state, player, hit)
        ? InteractionResult.sidedSuccess(level.isClientSide)
        : InteractionResult.PASS;
  }

  public boolean harvest(
      Level level, BlockPos pos, BlockState state, Player player, BlockHitResult hit) {
    pos = rootPos(state, pos);
    state = level.getBlockState(pos);
    if (!state.is(this)
        || !isMaxAge(state)
        || !complete(level, pos, state)
        || !level.mayInteract(player, pos)
        || (state.getValue(TRELLISED) && !level.mayInteract(player, pos.above()))) return false;
    double x = hit.getLocation().x - pos.getX(), z = hit.getLocation().z - pos.getZ();
    double across =
        switch (state.getValue(FACING)) {
          case NORTH -> x;
          case SOUTH -> 1 - x;
          case EAST -> z;
          case WEST -> 1 - z;
          default -> x;
        };
    int cluster = across < 0.5 ? 1 : 2;
    if ((state.getValue(BUNCHES) & cluster) == 0) return false;
    if (!level.isClientSide) {
      int remaining = state.getValue(BUNCHES) & ~cluster;
      updatePair(
          level,
          pos,
          remaining == 0
              ? state.setValue(AGE, REGROWTH_AGE).setValue(BUNCHES, 3)
              : state.setValue(BUNCHES, remaining));
      popResource(
          level,
          pos,
          new ItemStack(
              variety.grapeItem(), state.getValue(TRELLISED) ? 1 + level.random.nextInt(2) : 1));
      level.playSound(
          null,
          pos,
          SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
          SoundSource.BLOCKS,
          1,
          0.8F + level.random.nextFloat() * 0.4F);
    }
    return true;
  }

  @Override
  protected void entityInside(
      BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
    // Grapes are permanent woody vines, not annual crops trampled by ravagers.
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    if (!state.getValue(TRELLISED))
      return box(1, 0, 1, 15, Math.min(15, 4 + getAge(state) * 2), 15);
    return Shapes.or(
        GrapeTrellisBlock.shape(state),
        state.getValue(HALF) == DoubleBlockHalf.UPPER
            ? box(1, 0, 2, 15, 15, 14)
            : box(6, 0, 6, 10, 16, 10));
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return state.getValue(TRELLISED) ? GrapeTrellisBlock.shape(state) : Shapes.empty();
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
    if (!replacement.is(this) && state.getValue(TRELLISED))
      GrapeTrellisBlock.removePartner(state, level, pos);
    super.onRemove(state, level, pos, replacement, moving);
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
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AGE, HALF, FACING, TRELLISED, BUNCHES);
  }
}
