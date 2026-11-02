package io.vinum.tree;

import io.vinum.item.ModItems;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

/** Axe use peels cork; the living trunk stays intact and displays exposed reddish bark. */
public final class CorkLogBlock extends RotatedPillarBlock implements EntityBlock {
  public static final BooleanProperty HARVESTED = BooleanProperty.create("harvested");

  public CorkLogBlock(Properties properties) {
    super(properties);
    registerDefaultState(defaultBlockState().setValue(HARVESTED, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(HARVESTED);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new CorkLogBlockEntity(pos, state);
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
    if (!held.canPerformAction(ItemAbilities.AXE_STRIP))
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    if (!player.mayBuild() || !level.mayInteract(player, pos)) return ItemInteractionResult.FAIL;
    if (level.isClientSide) return ItemInteractionResult.SUCCESS;
    if (!(level.getBlockEntity(pos) instanceof CorkLogBlockEntity bark))
      return ItemInteractionResult.FAIL;
    bark.refresh();
    if (!bark.harvest()) {
      player.displayClientMessage(
          net.minecraft.network.chat.Component.translatable("message.vinum.cork_regrowing"), true);
      return ItemInteractionResult.CONSUME;
    }
    if (!player.hasInfiniteMaterials()) {
      popResource(level, pos, new ItemStack(ModItems.CORK_BARK.get(), 2 + level.random.nextInt(2)));
      held.hurtAndBreak(
          1,
          player,
          hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }
    level.playSound(
        null,
        pos,
        net.minecraft.sounds.SoundEvents.AXE_STRIP,
        net.minecraft.sounds.SoundSource.BLOCKS,
        1,
        .9F);
    return ItemInteractionResult.CONSUME;
  }

  @Override
  protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    if (level.getBlockEntity(pos) instanceof CorkLogBlockEntity bark) {
      bark.refresh();
      bark.schedule();
    }
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (level.getBlockEntity(pos) instanceof CorkLogBlockEntity bark && bark.harvested()) {
      level.setBlock(pos, state.setValue(HARVESTED, true), 3);
      bark.schedule();
    }
  }

  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    List<ItemStack> drops = super.getDrops(state, params);
    if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        instanceof CorkLogBlockEntity bark)
      for (ItemStack stack : drops)
        if (stack.is(CorkOakContent.LOG_ITEM.get()))
          bark.saveToItem(stack, params.getLevel().registryAccess());
    return drops;
  }

  @Override
  public ItemStack getCloneItemStack(
      BlockState state,
      net.minecraft.world.phys.HitResult target,
      LevelReader level,
      BlockPos pos,
      Player player) {
    ItemStack stack = new ItemStack(CorkOakContent.LOG_ITEM.get());
    if (level.getBlockEntity(pos) instanceof CorkLogBlockEntity bark)
      bark.saveToItem(stack, level.registryAccess());
    return stack;
  }
}
