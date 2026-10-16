package io.vinum.barrel;

import javax.annotation.Nullable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/** Immutable inspection snapshot for future screens, recipes and integrations. */
public record BarrelContents(FluidStack fluid, int capacity, long fluidContentAge, boolean available) {
    public BarrelContents {
        fluid = fluid.copy();
        fluidContentAge = fluid.isEmpty() ? 0 : Math.max(0, fluidContentAge);
    }

    @Override
    public FluidStack fluid() { return fluid.copy(); }

    public int amount() { return fluid.getAmount(); }

    /** Empty/unavailable reservoirs have no fluid type. */
    @Nullable
    public FluidType fluidType() { return fluid.isEmpty() ? null : fluid.getFluidType(); }
}
