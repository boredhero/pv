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
    /** Transfer between copies and commit both hands together, preserving partial volumes and metadata. */
    public static boolean betweenHands(Level level,Player player,InteractionHand hand){
        InteractionHand other=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        ItemStack held=player.getItemInHand(hand), opposite=player.getItemInHand(other);
        if(!(held.getItem() instanceof FluidContainerItem a) || !(opposite.getItem() instanceof FluidContainerItem b)
            || held.getCount()!=1 || opposite.getCount()!=1)return false;
        var first=a.handler(held.copy());var second=b.handler(opposite.copy());
        IFluidHandler source=player.isShiftKeyDown()?first:second, target=player.isShiftKeyDown()?second:first;
        FluidStack sample=source.drain(Integer.MAX_VALUE,FluidAction.SIMULATE);
        ItemStack receiving = player.isShiftKeyDown() ? second.getContainer() : first.getContainer();
        boolean corkNeeded = BottleCorks.requiresCork(receiving, sample);
        if (!BottleCorks.prepare(receiving, sample, player)) return false;
        int amount=target.fill(sample,FluidAction.SIMULATE);
        if(amount<=0)return false;
        if(level.isClientSide)return true;
        FluidStack drained=source.drain(amount,FluidAction.EXECUTE);
        if(drained.getAmount()!=amount || target.fill(drained,FluidAction.EXECUTE)!=amount)return false;
        if (corkNeeded) BottleCorks.consume(player);
        player.setItemInHand(hand,first.getContainer());player.setItemInHand(other,second.getContainer());
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.PLAYERS,.7F,1);
        return true;
    }
    public static ItemInteractionResult use(ItemStack held, Level level, Player player, InteractionHand hand, IFluidHandler tank) {
        return use(held, level, player, hand, tank, player.isShiftKeyDown());
    }
    /** Explicit direction for interactions whose receiving vessel determines the action. */
    public static ItemInteractionResult use(ItemStack held, Level level, Player player, InteractionHand hand,
            IFluidHandler tank, boolean pouring) {
        if (!(held.getItem() instanceof FluidContainerItem item)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        var container = item.handler(held.copyWithCount(1));
        IFluidHandler source = pouring ? container : tank;
        IFluidHandler target = pouring ? tank : container;
        FluidStack sample = source.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        boolean corkNeeded = !pouring && BottleCorks.requiresCork(container.getContainer(), sample);
        if (!pouring && !BottleCorks.prepare(container.getContainer(), sample, player)) return ItemInteractionResult.FAIL;
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
        if (filled <= 0) return ItemInteractionResult.FAIL;
        if (corkNeeded) BottleCorks.consume(player);
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
