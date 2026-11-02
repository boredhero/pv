package io.vinum.production;

import io.vinum.barrel.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/** Pure batch conversion. The vessel owns clocks, heat, starters and recipe snapshots. */
public final class FermentationEngine {
    private FermentationEngine() {}
    public static FluidStack finish(FluidStack input, FermentationRecipe recipe) {
        if (!recipe.matches(input)) return FluidStack.EMPTY;
        BatchProfile profile = BatchProfile.of(input);
        // Cap by available sugar and the remaining alcohol fraction; never create alcohol from absent sugar.
        double consumed = Math.min(profile.sugarGramsPerLitre(), recipe.sugarConsumedGramsPerLitre());
        if (recipe.alcoholYieldFraction() > 0) consumed = Math.min(consumed, (1 - profile.alcoholFraction()) / recipe.alcoholYieldFraction());
        BatchProfile result = new BatchProfile(Math.max(0, profile.sugarGramsPerLitre() - consumed), profile.starchGramsPerLitre(),
                profile.solidsGramsPerLitre() * recipe.solidsRemainingFraction(),
                Math.min(1, profile.alcoholFraction() + consumed * recipe.alcoholYieldFraction()),
                profile.ingredientProportions(), profile.yeast(), profile.history()).addHistory(ProductionInputs.id("fermentation"));
        FluidStack output = new FluidStack(BuiltInRegistries.FLUID.get(recipe.outputFluid()), input.getAmount());
        output.applyComponents(input.getComponentsPatch());
        output.remove(ModDataComponents.FLUID_AGE.get()); // Maturation starts after fermentation.
        return result.applyTo(output);
    }
}
