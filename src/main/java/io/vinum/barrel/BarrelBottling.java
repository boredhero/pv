package io.vinum.barrel;

import java.util.Optional;
import io.vinum.production.BatchProfile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Exact-volume container transactions. Simulations never consume items or change the barrel. */
public final class BarrelBottling {
    private BarrelBottling() {}

    public static ItemInteractionResult useItemOn(ItemStack held, Level level, Player player,
                                                  InteractionHand hand, IFluidHandler tank) {
        Optional<BeverageCatalog.FilledServing> filled = BeverageCatalog.lookupFilled(held);
        if (filled.isPresent()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            var match = filled.get();
            FluidStack contents = restoreContents(held, new FluidStack(match.beverage().fluid().get(), match.serving().volume()),
                    match.serving().age(held));
            if (contents.isEmpty()) return ItemInteractionResult.FAIL;
            return pour(held, level, player, hand, tank, contents, new ItemStack(match.serving().emptyItem().get()));
        }
        if (BeverageCatalog.isEmptyServing(held)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            FluidStack sample = tank.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            Optional<BeverageDefinition> beverage = BeverageCatalog.lookupFluid(sample.getFluid());
            if (sample.isEmpty() || beverage.isEmpty() || BatchProfile.of(sample).solidsGramsPerLitre() > 0) return ItemInteractionResult.FAIL;
            Optional<Serving> serving = beverage.get().servings().stream()
                    .filter(candidate -> held.is(candidate.emptyItem().get())).findFirst();
            if (serving.isEmpty() || sample.getAmount() < serving.get().volume()) return ItemInteractionResult.FAIL;
            FluidStack requested = sample.copyWithAmount(serving.get().volume());
            ItemStack result = serving.get().createFilled(BarrelFluidData.getAge(sample));
            return draw(held, level, player, hand, tank, requested, result);
        }
        // Vanilla bucket wrappers discard arbitrary fluid components; preserve the complete batch.
        if (held.is(Items.WATER_BUCKET) || held.is(Items.LAVA_BUCKET)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            FluidStack contents = FluidUtil.getFluidContained(held).orElse(FluidStack.EMPTY);
            if (contents.isEmpty()) return ItemInteractionResult.FAIL;
            contents = restoreContents(held, contents, held.getOrDefault(ModDataComponents.FLUID_AGE.get(), 0L));
            if (contents.isEmpty()) return ItemInteractionResult.FAIL;
            return pour(held, level, player, hand, tank, contents, new ItemStack(Items.BUCKET));
        }
        if (held.is(Items.BUCKET)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            FluidStack sample = tank.drain(1000, FluidAction.SIMULATE);
            if (sample.getAmount() != 1000 || sample.getFluid().getBucket() == Items.AIR) return ItemInteractionResult.FAIL;
            // Other mods' bucket/container capabilities keep their normal interaction path.
            if (sample.getFluid().getBucket() != Items.WATER_BUCKET && sample.getFluid().getBucket() != Items.LAVA_BUCKET) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            ItemStack result = new ItemStack(sample.getFluid().getBucket());
            long age = BarrelFluidData.getAge(sample);
            if (age > 0) result.set(ModDataComponents.FLUID_AGE.get(), age);
            return draw(held, level, player, hand, tank, sample, result);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult pour(ItemStack held, Level level, Player player, InteractionHand hand,
                                               IFluidHandler tank, FluidStack contents, ItemStack empty) {
        int amount = contents.getAmount();
        if (tank.fill(contents, FluidAction.SIMULATE) != amount) return ItemInteractionResult.FAIL;
        if (tank.fill(contents, FluidAction.EXECUTE) != amount) {
            throw new IllegalStateException("Fluid handler changed capacity during one barrel container transaction");
        }
        exchange(held, player, hand, empty, false);
        feedback(level, player, false);
        return ItemInteractionResult.CONSUME;
    }

    private static ItemInteractionResult draw(ItemStack held, Level level, Player player, InteractionHand hand,
                                               IFluidHandler tank, FluidStack requested, ItemStack result) {
        FluidStack simulated = tank.drain(requested, FluidAction.SIMULATE);
        if (simulated.getAmount() != requested.getAmount()) return ItemInteractionResult.FAIL;
        FluidStack drained = tank.drain(requested, FluidAction.EXECUTE);
        if (drained.getAmount() != requested.getAmount()) {
            // Defensive rollback for a nonconforming external handler; our barrel operates on the server thread.
            if (!drained.isEmpty()) tank.fill(drained, FluidAction.EXECUTE);
            return ItemInteractionResult.FAIL;
        }
        preserveContents(result, drained);
        exchange(held, player, hand, result, true);
        feedback(level, player, true);
        return ItemInteractionResult.CONSUME;
    }

    private static FluidStack restoreContents(ItemStack container, FluidStack expected, long age) {
        SimpleFluidContent stored = container.get(ModDataComponents.CONTAINED_FLUID.get());
        if (stored == null) return BarrelFluidData.withAge(expected, age);
        // Container definitions, not item metadata, determine which fluid and volume may be poured.
        if (stored.isEmpty() || !expected.getFluid().isSame(stored.getFluid()) || stored.getAmount() != expected.getAmount()) {
            return FluidStack.EMPTY;
        }
        return BarrelFluidData.withAge(stored.copy(), age);
    }

    private static void preserveContents(ItemStack container, FluidStack contents) {
        long age = BarrelFluidData.getAge(contents);
        if (age > 0) container.set(ModDataComponents.FLUID_AGE.get(), age);
        else container.remove(ModDataComponents.FLUID_AGE.get());

        // Keep the existing compact age component canonical and ordinary servings stack-compatible.
        // Store an immutable fluid snapshot only for additional metadata (quality, ABV, addon data...).
        FluidStack extra = contents.copy();
        extra.remove(ModDataComponents.FLUID_AGE.get());
        if (extra.isComponentsPatchEmpty()) container.remove(ModDataComponents.CONTAINED_FLUID.get());
        else container.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(extra));
    }

    private static void exchange(ItemStack held, Player player, InteractionHand hand, ItemStack result, boolean drawing) {
        if (player.hasInfiniteMaterials()) {
            // Match creative bucket behavior: retain the original, never spawn extra empty containers.
            if (drawing && !player.getInventory().contains(result)) give(player, result);
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, result);
        else give(player, result);
    }

    private static void give(Player player, ItemStack result) {
        if (!player.getInventory().add(result)) player.drop(result, false);
    }
    private static void feedback(Level level, Player player, boolean drawing) {
        level.playSound(null, player.blockPosition(), drawing ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY,
                SoundSource.BLOCKS, 0.7F, 1.0F);
        level.gameEvent(player, drawing ? GameEvent.FLUID_PICKUP : GameEvent.FLUID_PLACE, player.blockPosition());
    }
}
