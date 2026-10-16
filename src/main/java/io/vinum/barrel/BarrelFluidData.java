package io.vinum.barrel;

import java.math.BigInteger;
import net.neoforged.neoforge.fluids.FluidStack;

/** Age is elapsed world ticks, carried as a snapshot on transferred fluid. */
public final class BarrelFluidData {
    private BarrelFluidData() {}

    public static long getAge(FluidStack fluid) {
        return fluid.isEmpty() ? 0 : Math.max(0L, fluid.getOrDefault(ModDataComponents.FLUID_AGE.get(), 0L));
    }

    public static FluidStack withAge(FluidStack fluid, long age) {
        if (fluid.isEmpty()) return FluidStack.EMPTY;
        FluidStack result = fluid.copy();
        // Keep fresh fluids component-compatible with ordinary untagged containers.
        if (age <= 0) result.remove(ModDataComponents.FLUID_AGE.get());
        else result.set(ModDataComponents.FLUID_AGE.get(), age);
        return result;
    }

    public static boolean sameFluid(FluidStack first, FluidStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        if (!FluidStack.isSameFluid(first, second)) return false;
        FluidStack a = first.copy();
        FluidStack b = second.copy();
        a.remove(ModDataComponents.FLUID_AGE.get());
        b.remove(ModDataComponents.FLUID_AGE.get());
        return FluidStack.isSameFluidSameComponents(a, b);
    }

    public static long weightedAge(long firstAge, long firstAmount, long secondAge, long secondAmount) {
        long amount = firstAmount + secondAmount;
        if (amount <= 0) return 0;
        return BigInteger.valueOf(Math.max(0, firstAge)).multiply(BigInteger.valueOf(firstAmount))
                .add(BigInteger.valueOf(Math.max(0, secondAge)).multiply(BigInteger.valueOf(secondAmount)))
                .divide(BigInteger.valueOf(amount)).longValueExact();
    }

    public static long elapsedAge(long age, long referenceTick, long now) {
        long elapsed = referenceTick < 0 ? 0 : Math.max(0, now - referenceTick);
        return age > Long.MAX_VALUE - elapsed ? Long.MAX_VALUE : age + elapsed;
    }
}
