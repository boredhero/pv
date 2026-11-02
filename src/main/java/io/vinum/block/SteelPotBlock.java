package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.item.ModItems;
import io.vinum.util.VoxelShapeHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;

public class SteelPotBlock extends SteelComponentBlock {
  public static final MapCodec<SteelPotBlock> CODEC = simpleCodec(SteelPotBlock::new);

  /** Zero is empty, 1..100 evaporates for 100 heated ticks, 101 holds collectible salt. */
  public static final IntegerProperty EVAPORATION = IntegerProperty.create("evaporation", 0, 101);

  public static final int SALT_READY = 101;

  public SteelPotBlock(Properties properties) {
    super(
        properties,
        Shapes.or(
            Shapes.or(
                Shapes.or(
                    VoxelShapeHelper.box(1.0D, 2.0D, 1.0D, 15.0D, 14.0D, 15.0D),
                    VoxelShapeHelper.box(3.0D, 1.0D, 3.0D, 13.0D, 2.0D, 13.0D)),
                Shapes.or(
                    Shapes.or(
                        VoxelShapeHelper.box(4.0D, 1.0D, 2.0D, 12.0D, 2.0D, 3.0D),
                        VoxelShapeHelper.box(2.0D, 1.0D, 4.0D, 3.0D, 2.0D, 12.0D)),
                    Shapes.or(
                        VoxelShapeHelper.box(4.0D, 1.0D, 14.0D, 12.0D, 2.0D, 13.0D),
                        VoxelShapeHelper.box(14.0D, 1.0D, 4.0D, 13.0D, 2.0D, 12.0D))),
                Shapes.or(
                    Shapes.or(
                        VoxelShapeHelper.box(2.0D, 0.0D, 2.0D, 4.0D, 1.0D, 4.0D),
                        VoxelShapeHelper.box(14.0D, 0.0D, 2.0D, 12.0D, 1.0D, 4.0D)),
                    Shapes.or(
                        VoxelShapeHelper.box(2.0D, 0.0D, 14.0D, 4.0D, 1.0D, 12.0D),
                        VoxelShapeHelper.box(14.0D, 0.0D, 14.0D, 12.0D, 1.0D, 12.0D))))));
  }

  public static boolean isPotEmpty(BlockState state) {
    return !state.hasProperty(EVAPORATION) || state.getValue(EVAPORATION) == 0;
  }

  private static boolean heated(Level level, BlockPos pos) {
    BlockState below = level.getBlockState(pos.below());
    return below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT)
        || below.getBlock() instanceof SteelBrazierBlock
            && below.getValue(SteelBrazierBlock.LIT)
            && !below.getValue(WATERLOGGED);
  }

  @Override
  protected ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      InteractionHand hand,
      BlockHitResult hit) {
    if (!stack.is(Items.WATER_BUCKET))
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    if (!level.mayInteract(player, pos)) return ItemInteractionResult.FAIL;
    if (!isPotEmpty(state) || state.getValue(WATERLOGGED) || !heated(level, pos)) {
      if (!level.isClientSide)
        player.displayClientMessage(
            Component.translatable(
                isPotEmpty(state) ? "message.vinum.salt_needs_heat" : "message.vinum.salt_busy"),
            true);
      return ItemInteractionResult.FAIL;
    }
    if (!level.isClientSide) {
      level.setBlock(pos, state.setValue(EVAPORATION, 1), UPDATE_CLIENTS);
      player.setItemInHand(
          hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
      level.scheduleTick(pos, this, 1);
      player.displayClientMessage(Component.translatable("message.vinum.salt_evaporating"), true);
    }
    return ItemInteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (isPotEmpty(state) || !level.mayInteract(player, pos)) return InteractionResult.PASS;
    if (!level.isClientSide) {
      if (state.getValue(EVAPORATION) == SALT_READY) {
        level.setBlock(pos, state.setValue(EVAPORATION, 0), UPDATE_CLIENTS);
        ItemStack salt = new ItemStack(ModItems.SEA_SALT.get());
        if (!player.getInventory().add(salt)) player.drop(salt, false);
        player.displayClientMessage(Component.translatable("message.vinum.salt_collected"), true);
      } else {
        player.displayClientMessage(
            Component.translatable(
                heated(level, pos) && !state.getValue(WATERLOGGED)
                    ? "message.vinum.salt_evaporating"
                    : "message.vinum.salt_needs_heat"),
            true);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    int progress = state.getValue(EVAPORATION);
    if (progress == 0 || progress == SALT_READY) return;
    boolean hot = !state.getValue(WATERLOGGED) && heated(level, pos);
    if (hot) {
      progress++;
      level.setBlock(pos, state.setValue(EVAPORATION, progress), UPDATE_CLIENTS);
    }
    if (progress < SALT_READY) level.scheduleTick(pos, this, hot ? 1 : 20);
  }

  @Override
  protected void onPlace(
      BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
    super.onPlace(state, level, pos, old, moving);
    if (!level.isClientSide && !isPotEmpty(state) && state.getValue(EVAPORATION) < SALT_READY)
      level.scheduleTick(pos, this, 1);
  }

  @Override
  public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
    if (!isPotEmpty(state)
        && state.getValue(EVAPORATION) < SALT_READY
        && heated(level, pos)
        && !state.getValue(WATERLOGGED))
      level.addParticle(
          ParticleTypes.CLOUD, pos.getX() + .5, pos.getY() + .9, pos.getZ() + .5, 0, .015, 0);
  }

  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
    if (state.getValue(EVAPORATION) == SALT_READY && !drops.isEmpty())
      drops.add(new ItemStack(ModItems.SEA_SALT.get()));
    return drops;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(EVAPORATION);
  }

  @Override
  public MapCodec<SteelPotBlock> codec() {
    return CODEC;
  }
}
