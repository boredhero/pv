package io.vinum.beverage;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.ModDataComponents;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionDataComponents;
import java.util.HashSet;
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared conservative mixing policy. Only wine permits different batch profiles; unknown metadata must agree. */
public final class FluidBlending {
    private FluidBlending() {}
    public static boolean canBlend(FluidStack first, FluidStack second) {
        if (first.isEmpty() || second.isEmpty()) return true;
        FluidStack a = WineCatalog.normalize(first), b = WineCatalog.normalize(second);
        if (!FluidStack.isSameFluid(a, b)) return false;
        a.remove(ModDataComponents.FLUID_AGE.get()); b.remove(ModDataComponents.FLUID_AGE.get());
        if (WineCatalog.isWine(a)) {
            var keys = new HashSet<>(BatchProfile.of(a).ingredientProportions().keySet());
            keys.addAll(BatchProfile.of(b).ingredientProportions().keySet());
            if (BatchProfile.of(a).ingredientProportions().isEmpty() || BatchProfile.of(b).ingredientProportions().isEmpty())
                keys.add(io.vinum.production.ProductionInputs.id("unspecified"));
            if (keys.size() > BatchProfile.MAX_INGREDIENTS) return false;
            a.remove(ProductionDataComponents.BATCH_PROFILE.get()); b.remove(ProductionDataComponents.BATCH_PROFILE.get());
        }
        return FluidStack.isSameFluidSameComponents(a, b);
    }
    public static FluidStack blend(FluidStack first, FluidStack second) {
        if (!canBlend(first, second)) throw new IllegalArgumentException("Incompatible fluid batches");
        FluidStack a = WineCatalog.normalize(first), b = WineCatalog.normalize(second);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        int amount = Math.addExact(a.getAmount(), b.getAmount());
        FluidStack result = a.copyWithAmount(amount);
        if (WineCatalog.isWine(a)) BatchProfile.blend(BatchProfile.of(a), a.getAmount(), BatchProfile.of(b), b.getAmount()).applyTo(result);
        return BarrelFluidData.withAge(result, BarrelFluidData.weightedAge(BarrelFluidData.getAge(a), a.getAmount(),
                BarrelFluidData.getAge(b), b.getAmount()));
    }
}
