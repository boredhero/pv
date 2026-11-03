package io.vinum.production;

import io.vinum.fluid.ModFluids;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Cold preparation: one vanilla honey bottle contributes 250 mB and 250 g fermentable sugar.
 * Exact water batches keep the recipe unambiguous; prepared batches can be combined in vessels.
 */
public final class HoneyMixing {
    private HoneyMixing() {}
    public static FluidStack prepare(FluidStack water) {
        if (water.getFluid() != Fluids.WATER || water.getAmount() != 1000
                || !water.isComponentsPatchEmpty()) return FluidStack.EMPTY;
        FluidStack must = new FluidStack(ModFluids.MEAD_MUST.source().get(), 1250);
        new BatchProfile(200, 0, 0, 0, Map.of(ProductionInputs.id("honey"), 1.0),
                Optional.empty(), List.of(ProductionInputs.id("honey_mixing"))).applyTo(must);
        return must;
    }
}
