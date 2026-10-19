package io.vinum.production;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fermentation conversion is per litre, so every vessel size has identical chemistry.
 * alcoholYieldFraction is ABV gained per gram of sugar consumed per litre (e.g. 0.0006).
 */
public record FermentationRecipe(ResourceLocation inputFluid, ResourceLocation outputFluid, int durationTicks,
        double sugarConsumedGramsPerLitre, double alcoholYieldFraction, double solidsRemainingFraction)
        implements FluidProcessRecipe {
    public static final MapCodec<FermentationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("input_fluid").forGetter(FermentationRecipe::inputFluid),
            ResourceLocation.CODEC.fieldOf("output_fluid").forGetter(FermentationRecipe::outputFluid),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration_ticks").forGetter(FermentationRecipe::durationTicks),
            Codec.doubleRange(0, 10000).fieldOf("sugar_consumed_g_per_litre").forGetter(FermentationRecipe::sugarConsumedGramsPerLitre),
            Codec.doubleRange(0, 0.001).fieldOf("alcohol_yield_fraction").forGetter(FermentationRecipe::alcoholYieldFraction),
            Codec.doubleRange(0, 1).fieldOf("solids_remaining_fraction").forGetter(FermentationRecipe::solidsRemainingFraction)
    ).apply(instance, FermentationRecipe::new));
    public FermentationRecipe {
        if (durationTicks <= 0 || !Double.isFinite(sugarConsumedGramsPerLitre) || sugarConsumedGramsPerLitre < 0
                || sugarConsumedGramsPerLitre > 10000 || !Double.isFinite(alcoholYieldFraction) || alcoholYieldFraction < 0
                || alcoholYieldFraction > 0.001 || !Double.isFinite(solidsRemainingFraction)
                || solidsRemainingFraction < 0 || solidsRemainingFraction > 1) throw new IllegalArgumentException("Invalid fermentation recipe");
    }
    public boolean matches(FluidStack input) {
        return !input.isEmpty() && BuiltInRegistries.FLUID.getKey(input.getFluid()).equals(inputFluid)
                && BuiltInRegistries.FLUID.containsKey(outputFluid)
                && BuiltInRegistries.FLUID.get(outputFluid) != Fluids.EMPTY;
    }
    public FluidStack output(FluidStack input) { return FermentationEngine.finish(input, this); }
    @Override public RecipeSerializer<?> getSerializer() { return ProductionRecipes.FERMENTATION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ProductionRecipes.FERMENTATION_TYPE.get(); }
}
