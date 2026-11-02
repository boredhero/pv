package io.vinum.straining;

import com.mojang.serialization.MapCodec;
import io.vinum.item.FluidContainerItem;
import io.vinum.production.ProductionContainers;
import io.vinum.production.ProductionInputs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Stateless mesh workstation: copied containers are committed together only after complete separation. */
public final class StrainerBlock extends Block {
  public static final MapCodec<StrainerBlock> CODEC = simpleCodec(StrainerBlock::new);
  private static final VoxelShape SHAPE = Shapes.or(box(1, 9, 1, 15, 12, 15),
      box(2, 0, 2, 4, 9, 4), box(12, 0, 2, 14, 9, 4),
      box(2, 0, 12, 4, 9, 14), box(12, 0, 12, 14, 9, 14));
  public StrainerBlock(Properties properties) { super(properties); }
  @Override public MapCodec<StrainerBlock> codec() { return CODEC; }
  @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

  public static FluidStack strain(Level level, FluidStack input) {
    return level.getRecipeManager().getAllRecipesFor(StrainerContent.TYPE.get()).stream()
        .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
        .map(holder -> holder.value()).filter(recipe -> recipe.accepts(input))
        .findFirst().map(recipe -> recipe.strain(input)).orElse(FluidStack.EMPTY);
  }

  @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
      BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (!player.mayBuild() || !level.mayInteract(player, pos)
        || !player.mayUseItemAt(pos, hit.getDirection(), held)) return ItemInteractionResult.FAIL;
    if (held.getItem() instanceof FluidContainerItem container && held.getCount() == 1) {
      FluidStack output = strain(level, container.contents(held));
      if (output.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      var copy = container.handler(held.copy());
      copy.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
      // Straining treats the same filling; it neither consumes nor supplies a new cork.
      if (io.vinum.beverage.BottleCorks.hasCork(held))
        copy.getContainer().set(io.vinum.barrel.ModDataComponents.BOTTLE_CORK.get(), true);
      if (copy.fill(output, FluidAction.EXECUTE) != output.getAmount()) return ItemInteractionResult.FAIL;
      if (!level.isClientSide) {
        player.setItemInHand(hand, copy.getContainer());
        feedback(level, pos, player);
      }
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    // Prepared must is a bowl, not a reusable fluid container. Strain into the other hand.
    if (!ProductionContainers.emptyContainer(held).is(Items.BOWL)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    FluidStack output = strain(level, ProductionInputs.fromItem(held));
    if (output.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    ItemStack receiving = player.getItemInHand(other);
    if (!(receiving.getItem() instanceof FluidContainerItem receiver) || receiving.getCount() != 1) {
      if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.vinum.strainer_receiver"), true);
      return ItemInteractionResult.FAIL;
    }
    var copy = receiver.handler(receiving.copy());
    if (copy.fill(output, FluidAction.SIMULATE) != output.getAmount()) return ItemInteractionResult.FAIL;
    if (!level.isClientSide) {
      copy.fill(output, FluidAction.EXECUTE);
      player.setItemInHand(other, copy.getContainer());
      held.shrink(1);
      ItemStack bowl = new ItemStack(Items.BOWL);
      if (held.isEmpty()) player.setItemInHand(hand, bowl);
      else if (!player.getInventory().add(bowl)) player.drop(bowl, false);
      feedback(level, pos, player);
    }
    return ItemInteractionResult.sidedSuccess(level.isClientSide);
  }

  private static void feedback(Level level, BlockPos pos, Player player) {
    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, .7F, .9F);
    player.displayClientMessage(Component.translatable("message.vinum.strained"), true);
  }
  @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.vinum.strainer_help"), true);
    return InteractionResult.sidedSuccess(level.isClientSide);
  }
}
