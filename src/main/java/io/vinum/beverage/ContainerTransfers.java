package io.vinum.beverage;

import io.vinum.item.FluidContainerItem;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Normal use draws into the held container; sneak-use pours it. Partial transfers conserve remainders. */
public final class ContainerTransfers {
    private ContainerTransfers() {}
    public static ItemInteractionResult use(ItemStack held, Level level, Player player, InteractionHand hand, IFluidHandler tank) {
        if (!(held.getItem() instanceof FluidContainerItem item)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        var container = item.handler(held.copyWithCount(1));
        boolean pouring = player.isShiftKeyDown();
        IFluidHandler source = pouring ? container : tank;
        IFluidHandler target = pouring ? tank : container;
        FluidStack sample = source.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        int amount = target.fill(sample, FluidAction.SIMULATE);
        if (amount <= 0) return ItemInteractionResult.FAIL;
        FluidStack drained = source.drain(sample.copyWithAmount(amount), FluidAction.EXECUTE);
        if (drained.getAmount() != amount) {
            if (!drained.isEmpty()) source.fill(drained, FluidAction.EXECUTE);
            return ItemInteractionResult.FAIL;
        }
        int filled = target.fill(drained, FluidAction.EXECUTE);
        if (filled != amount) {
            source.fill(drained.copyWithAmount(amount - filled), FluidAction.EXECUTE);
        }
        // These reusable vessels conserve liquid in creative too; no duplicating a partially filled vessel.
        ItemStack result = container.getContainer();
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!player.getInventory().add(result)) player.drop(result, false);
        level.playSound(null, player.blockPosition(), pouring ? net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY : net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                net.minecraft.sounds.SoundSource.PLAYERS, .7F, 1F);
        return ItemInteractionResult.CONSUME;
    }
}
