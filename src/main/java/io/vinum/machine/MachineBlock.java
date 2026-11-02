package io.vinum.machine;

import io.vinum.beverage.ContainerTransfers;
import io.vinum.item.ModItems;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * Oriented machine shell; inventories, stored juice and attached parts travel with its dropped
 * item.
 */
public final class MachineBlock extends Block implements EntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty LIT = BlockStateProperties.LIT;
  public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
  private final boolean generator;
  private static final VoxelShape PRESS =
      Shapes.or(box(1, 0, 1, 15, 5, 15), box(2, 5, 2, 14, 16, 14));

  public MachineBlock(Properties properties, boolean generator) {
    super(
        properties
            .noOcclusion()
            .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    this.generator = generator;
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false)
            .setValue(HALF, DoubleBlockHalf.LOWER));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, LIT, HALF);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    if (!generator
        && (context.getClickedPos().getY() >= context.getLevel().getMaxBuildHeight() - 1
            || !context
                .getLevel()
                .getBlockState(context.getClickedPos().above())
                .canBeReplaced(context))) return null;
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return generator
        ? box(1, 0, 1, 15, 15, 15)
        : state.getValue(HALF) == DoubleBlockHalf.UPPER
            ? Shapes.or(box(1, 0, 1, 15, 6, 15), box(7, 6, 7, 9, 11, 9))
            : PRESS;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return generator
        ? new BasicGeneratorBlockEntity(pos, state)
        : state.getValue(HALF) == DoubleBlockHalf.LOWER
            ? new WinePressBlockEntity(pos, state)
            : null;
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide) return null;
    return (world, pos, block, entity) -> {
      if (entity instanceof WinePressBlockEntity press)
        WinePressBlockEntity.tick(world, pos, block, press);
      else if (entity instanceof BasicGeneratorBlockEntity gen)
        BasicGeneratorBlockEntity.tick(world, pos, block, gen);
    };
  }

  @Override
  protected ItemInteractionResult useItemOn(
      ItemStack held,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    pos = base(state, pos);
    if (level.getBlockEntity(pos) instanceof WinePressBlockEntity press) {
      if (held.is(ModItems.PRESS_MOTOR.get())) {
        if (!level.isClientSide && press.attachMotor() && !player.hasInfiniteMaterials())
          held.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
      if (held.is(ModItems.HAND_CRANK.get())) {
        if (!level.isClientSide && press.attachCrank() && !player.hasInfiniteMaterials())
          held.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
      if (FluidUtil.getFluidHandler(held).isPresent()) {
        var result = ContainerTransfers.use(held, level, player, hand, press.fluids());
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return result;
        return level.isClientSide
                || FluidUtil.interactWithFluidHandler(player, hand, press.fluids())
            ? ItemInteractionResult.sidedSuccess(level.isClientSide)
            : ItemInteractionResult.FAIL;
      }
    }
    if (held.getItem() instanceof BlockItem)
      return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    pos = base(state, pos);
    if (!level.isClientSide) {
      if (level.getBlockEntity(pos) instanceof WinePressBlockEntity press
          && press.hasCrank()
          && (player.isShiftKeyDown() || hit.getLocation().y - pos.getY() >= 1.0)) {
        boolean turned = press.turnCrank();
        if (turned)
          player.displayClientMessage(
              net.minecraft.network.chat.Component.translatable(
                  "gui.vinum.turn_progress",
                  press.requiredWork() == 0 ? 100 : press.data.get(1) * 100 / press.requiredWork()),
              true);
        else if (level.getGameTime() - press.lastCrank() >= WinePressBlockEntity.CRANK_COOLDOWN)
          player.displayClientMessage(
              net.minecraft.network.chat.Component.translatable("gui.vinum.crank_blocked"), true);
      } else if (level.getBlockEntity(pos) instanceof MenuProvider menu
          && player instanceof ServerPlayer server) {
        if (menu instanceof WinePressBlockEntity press)
          server.openMenu(menu, buffer -> buffer.writeBlockPos(press.getBlockPos()));
        else server.openMenu(menu);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  public static BlockPos base(BlockState state, BlockPos pos) {
    return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
  }

  @Override
  public void setPlacedBy(
      Level level,
      BlockPos pos,
      BlockState state,
      net.minecraft.world.entity.LivingEntity placer,
      ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (!generator) level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!state.is(replacement.getBlock()) && !generator) {
      BlockPos other = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
      BlockState paired = level.getBlockState(other);
      if (paired.is(this) && paired.getValue(HALF) != state.getValue(HALF)) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) level.destroyBlock(other, true);
        else level.removeBlock(other, false);
      }
    }
    super.onRemove(state, level, pos, replacement, moving);
  }

  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    if (!generator && state.getValue(HALF) == DoubleBlockHalf.UPPER) return List.of();
    List<ItemStack> drops = super.getDrops(state, params);
    if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlockEntity entity)
      for (ItemStack drop : drops)
        if (drop.is(asItem())) {
          entity.saveToItem(drop, params.getLevel().registryAccess());
          break;
        }
    return drops;
  }
}
