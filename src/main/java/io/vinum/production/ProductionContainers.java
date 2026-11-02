package io.vinum.production;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.ModDataComponents;
import io.vinum.item.ModItems;
import java.util.Optional;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Exact-volume production container transactions, shared by fermenters, barrels and machines. */
public final class ProductionContainers {
    public static final int BUCKET_VOLUME = 1_000;
    private ProductionContainers() {}

    public static Optional<FluidStack> read(ItemStack stack) {
        if (!stack.is(ModItems.BATCH_BUCKET.get())) {
            FluidStack prepared = ProductionInputs.fromItem(stack);
            return prepared.isEmpty() ? Optional.empty() : Optional.of(prepared);
        }
        SimpleFluidContent contents = stack.get(ModDataComponents.CONTAINED_FLUID.get());
        if (contents == null || contents.isEmpty() || contents.getAmount() > BUCKET_VOLUME) return Optional.empty();
        return Optional.of(BarrelFluidData.withAge(contents.copy(), stack.getOrDefault(ModDataComponents.FLUID_AGE.get(), 0L)));
    }

    /** Partial buckets are used only to preserve a machine's remainder when it is dismantled. */
    public static ItemStack create(FluidStack fluid) {
        if (fluid.isEmpty() || fluid.getAmount() > BUCKET_VOLUME) throw new IllegalArgumentException("Expected 1..1000 mB");
        ItemStack result = new ItemStack(ModItems.BATCH_BUCKET.get());
        result.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(fluid));
        long age = BarrelFluidData.getAge(fluid);
        if (age > 0) result.set(ModDataComponents.FLUID_AGE.get(), age);
        return result;
    }

    public static ItemStack emptyContainer(ItemStack stack) {
        if (stack.is(ModItems.BATCH_BUCKET.get())) return new ItemStack(Items.BUCKET);
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("grape_juice_")) return new ItemStack(Items.GLASS_BOTTLE);
        if (path.startsWith("grape_must_")) return new ItemStack(Items.BOWL);
        return ItemStack.EMPTY;
    }

    public static ItemInteractionResult useItemOn(ItemStack held, Level level, Player player,
                                                  InteractionHand hand, IFluidHandler tank) {
        Optional<FluidStack> source = read(held);
        if (source.isPresent()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            FluidStack fluid = source.get();
            if (tank.fill(fluid, FluidAction.SIMULATE) != fluid.getAmount()) return ItemInteractionResult.FAIL;
            if (tank.fill(fluid, FluidAction.EXECUTE) != fluid.getAmount()) throw new IllegalStateException("Non-atomic container fill");
            exchange(held, emptyContainer(held), player, hand, false);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, .7F, 1);
            return ItemInteractionResult.CONSUME;
        }
        if (!held.is(Items.BUCKET)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        FluidStack sample = tank.drain(BUCKET_VOLUME, FluidAction.SIMULATE);
        // Vanilla bucket interaction preserves its standard item IDs and behavior.
        if (sample.isEmpty() || sample.getFluid().getBucket() != Items.AIR) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (sample.getAmount() != BUCKET_VOLUME) return ItemInteractionResult.FAIL;
        FluidStack drained = tank.drain(sample, FluidAction.EXECUTE);
        if (drained.getAmount() != BUCKET_VOLUME) {
            if (!drained.isEmpty()) tank.fill(drained, FluidAction.EXECUTE);
            return ItemInteractionResult.FAIL;
        }
        exchange(held, create(drained), player, hand, true);
        level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, .7F, 1);
        return ItemInteractionResult.CONSUME;
    }

    private static void exchange(ItemStack held, ItemStack result, Player player, InteractionHand hand, boolean drawing) {
        if (player.hasInfiniteMaterials()) {
            if (drawing && !player.getInventory().contains(result)) give(result, player);
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else if (!result.isEmpty()) give(result, player);
    }

    private static void give(ItemStack result, Player player) {
        if (!player.getInventory().add(result)) player.drop(result, false);
    }
}
