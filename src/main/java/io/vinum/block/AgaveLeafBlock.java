package io.vinum.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.*;

/**
 * Non-dropping pieces refer to the root by bounded offsets, requiring no block entity or topology
 * search.
 */
public final class AgaveLeafBlock extends Block {
  public static final IntegerProperty X = IntegerProperty.create("root_x", 0, 2);
  public static final IntegerProperty Y = IntegerProperty.create("root_y", 0, 2);
  public static final IntegerProperty Z = IntegerProperty.create("root_z", 0, 2);

  public AgaveLeafBlock(Properties properties) {
    super(properties.noOcclusion().randomTicks());
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(X, 1)
            .setValue(Y, 0)
            .setValue(Z, 1)
            .setValue(AgaveCropBlock.AGE, 0));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(X, Y, Z, AgaveCropBlock.AGE);
  }

  public static BlockPos root(BlockState state, BlockPos pos) {
    return pos.offset(1 - state.getValue(X), -state.getValue(Y), 1 - state.getValue(Z));
  }

  public static boolean belongsTo(BlockState state, BlockPos pos, BlockPos root) {
    return state.getBlock() instanceof AgaveLeafBlock && root(state, pos).equals(root);
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return AgaveGeometry.shape(
        state.getValue(AgaveCropBlock.AGE),
        state.getValue(X) - 1,
        state.getValue(Y),
        state.getValue(Z) - 1);
  }

  private static boolean ownsCell(BlockState rootState, BlockPos offset) {
    return rootState.getBlock() instanceof AgaveCropBlock
        && !rootState.getValue(AgaveCropBlock.STRIPPED)
        && AgaveGeometry.cells(rootState.getValue(AgaveCropBlock.AGE)).contains(offset);
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      Direction side,
      BlockState neighbor,
      LevelAccessor level,
      BlockPos pos,
      BlockPos neighborPos) {
    BlockPos root = root(state, pos);
    if (!level.hasChunk(
        SectionPos.blockToSectionCoord(root.getX()), SectionPos.blockToSectionCoord(root.getZ())))
      return state;
    return ownsCell(level.getBlockState(root), pos.subtract(root))
        ? state
        : Blocks.AIR.defaultBlockState();
  }

  @Override
  public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    BlockPos root = root(state, pos);
    BlockState plant = level.getBlockState(root);
    if (!level.isClientSide
        && ownsCell(plant, pos.subtract(root))
        && (player.hasInfiniteMaterials() || plant.getValue(AgaveCropBlock.AGE) < 7)) {
      if (!player.hasInfiniteMaterials())
        Block.dropResources(
            plant, (ServerLevel) level, root, null, player, player.getMainHandItem());
      level.removeBlock(root, false);
    }
    return super.playerWillDestroy(level, pos, state, player);
  }

  @Override
  protected net.minecraft.world.ItemInteractionResult useItemOn(
      ItemStack held,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      net.minecraft.world.InteractionHand hand,
      net.minecraft.world.phys.BlockHitResult hit) {
    if (!(held.is(net.minecraft.tags.ItemTags.AXES)
            || held.is(net.minecraft.world.item.Items.SHEARS))
        || state.getValue(AgaveCropBlock.AGE) != 7)
      return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    if (!player.mayBuild()
        || !level.mayInteract(player, pos)
        || !player.mayUseItemAt(pos, hit.getDirection(), held))
      return net.minecraft.world.ItemInteractionResult.FAIL;
    if (level.isClientSide) return net.minecraft.world.ItemInteractionResult.SUCCESS;
    if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS
        .post(
            new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos, state, player))
        .isCanceled()) return net.minecraft.world.ItemInteractionResult.FAIL;
    if (!level.removeBlock(pos, false)) return net.minecraft.world.ItemInteractionResult.FAIL;
    level.levelEvent(2001, pos, Block.getId(state));
    if (!player.hasInfiniteMaterials())
      held.hurtAndBreak(
          1,
          player,
          hand == net.minecraft.world.InteractionHand.MAIN_HAND
              ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
              : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
    return net.minecraft.world.ItemInteractionResult.CONSUME;
  }

  @Override
  protected void randomTick(
      BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
    BlockPos root = root(state, pos);
    if (level.hasChunk(
            SectionPos.blockToSectionCoord(root.getX()),
            SectionPos.blockToSectionCoord(root.getZ()))
        && !ownsCell(level.getBlockState(root), pos.subtract(root))) level.removeBlock(pos, false);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!replacement.is(this)) {
      BlockPos root = root(state, pos);
      if (level.hasChunk(
              SectionPos.blockToSectionCoord(root.getX()),
              SectionPos.blockToSectionCoord(root.getZ()))
          && ownsCell(level.getBlockState(root), pos.subtract(root))) {
        if (level.getBlockState(root).getValue(AgaveCropBlock.AGE) == 7)
          AgaveCropBlock.refreshStripped(level, root);
        else level.destroyBlock(root, true);
      }
    }
    super.onRemove(state, level, pos, replacement, moving);
  }

  @Override
  public ItemStack getCloneItemStack(
      BlockState state, HitResult hit, LevelReader level, BlockPos pos, Player player) {
    return new ItemStack(io.vinum.item.ModItems.AGAVE_SEEDS.get());
  }
}
