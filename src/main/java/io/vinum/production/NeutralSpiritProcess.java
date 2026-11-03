package io.vinum.production;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Neutral-spirit production policy, independent of machines and storage implementations. */
public final class NeutralSpiritProcess {
    private static final double[] STRENGTHS = {.60, .80, .90, .95};
    private static final ResourceLocation NEUTRAL = ProductionInputs.id("neutral_spirit");
    private static final ResourceLocation VODKA = ProductionInputs.id("vodka");
    /** Matches the Blender's whole-millibucket target-ABV tolerance (0.05 percentage points). */
    public static final double BOTTLING_TOLERANCE = 0.0005;
    private NeutralSpiritProcess() {}

    /** Game balance stages, not a physical claim about a universal number of still runs. */
    public static double nextStrength(double current) {
        if (!Double.isFinite(current) || current <= 0) return 0;
        for (double strength : STRENGTHS)
            if (current < strength - 1e-9) return strength;
        return 0;
    }

    public static ResourceLocation historyEvent(double strength) {
        return ProductionInputs.id("rectification_" + Math.round(strength * 100));
    }

    /** Historical peak survives dilution; it is not a claim that blending removes congeners. */
    public static String refinementTooltip(FluidStack fluid) {
        if (!isNeutralFamily(fluid)) return "";
        var profile = BatchProfile.of(fluid);
        int peak = 0;
        for (double strength : STRENGTHS)
            if (profile.history().contains(historyEvent(strength))) peak = (int)Math.round(strength * 100);
        // Creative samples and supplied neutral spirits may have no processing history.
        if (peak == 0 && profile.history().isEmpty())
            for (double strength : STRENGTHS)
                if (profile.alcoholFraction() >= strength - 1e-9) peak = (int)Math.round(strength * 100);
        return "tooltip.vinum.rectification." + (peak == 0 ? "unknown" : peak);
    }

    public static boolean isNeutralFamily(FluidStack fluid) {
        if (fluid.isEmpty()) return false;
        var id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        return id.equals(NEUTRAL) || id.equals(VODKA);
    }

    /** Reclassifies only this family; diluting rum, brandy, or tequila never makes vodka. */
    public static FluidStack classify(FluidStack fluid) {
        if (!isNeutralFamily(fluid)) return fluid;
        double strength = BatchProfile.of(fluid).alcoholFraction();
        ResourceLocation target = Math.abs(strength - .40) <= BOTTLING_TOLERANCE + 1e-12
                ? VODKA : strength > .40 ? NEUTRAL : BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        if (BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(target)) return fluid;
        FluidStack result = new FluidStack(BuiltInRegistries.FLUID.get(target), fluid.getAmount());
        result.applyComponents(fluid.getComponentsPatch());
        return result;
    }
}
