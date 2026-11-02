package io.vinum.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Extension point for grain mashes: convert starch to sugar before fermentation. */
public record MashingRecipe(ResourceLocation inputFluid, ResourceLocation outputFluid, int durationTicks,
        double starchConversionFraction, boolean requiresHeat) implements FluidProcessRecipe {
    public static final MapCodec<MashingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("input_fluid").forGetter(MashingRecipe::inputFluid),
            ResourceLocation.CODEC.fieldOf("output_fluid").forGetter(MashingRecipe::outputFluid),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration_ticks").forGetter(MashingRecipe::durationTicks),
            Codec.doubleRange(0, 1).fieldOf("starch_conversion_fraction").forGetter(MashingRecipe::starchConversionFraction),
            Codec.BOOL.optionalFieldOf("requires_heat", true).forGetter(MashingRecipe::requiresHeat)
    ).apply(instance, MashingRecipe::new));
    public MashingRecipe {
        if (durationTicks <= 0 || !Double.isFinite(starchConversionFraction) || starchConversionFraction < 0 || starchConversionFraction > 1)
            throw new IllegalArgumentException("Invalid mash recipe");
    }
    public boolean matches(FluidStack input, boolean heated) {
        return !input.isEmpty() && (!requiresHeat || heated) && BuiltInRegistries.FLUID.getKey(input.getFluid()).equals(inputFluid)
                && BuiltInRegistries.FLUID.containsKey(outputFluid)
                && BuiltInRegistries.FLUID.get(outputFluid) != Fluids.EMPTY;
    }
    @Override public RecipeSerializer<?> getSerializer() { return ProductionRecipes.MASHING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ProductionRecipes.MASHING_TYPE.get(); }
}
