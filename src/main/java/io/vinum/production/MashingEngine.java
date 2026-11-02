package io.vinum.production;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MashingEngine {
    private MashingEngine() {}
    public static FluidStack finish(FluidStack input, MashingRecipe recipe, boolean heated) {
        if (!recipe.matches(input, heated)) return FluidStack.EMPTY;
        BatchProfile profile = BatchProfile.of(input);
        double converted = Math.min(profile.starchGramsPerLitre() * recipe.starchConversionFraction(),
                10000 - profile.sugarGramsPerLitre());
        var mashed = new BatchProfile(profile.sugarGramsPerLitre() + converted, profile.starchGramsPerLitre() - converted,
                profile.solidsGramsPerLitre(), profile.alcoholFraction(), profile.ingredientProportions(), profile.yeast(), profile.history())
                .addHistory(ProductionInputs.id("mashing"));
        FluidStack output = new FluidStack(BuiltInRegistries.FLUID.get(recipe.outputFluid()), input.getAmount());
        output.applyComponents(input.getComponentsPatch());
        return mashed.applyTo(output);
    }
}
