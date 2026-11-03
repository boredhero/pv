package io.vinum.straining;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.production.BatchProfile;
import io.vinum.production.FluidProcessRecipe;
import io.vinum.production.ProductionInputs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Volume-neutral separation. Recipes declare fluid identities; composition remains on the batch. */
public record StrainingRecipe(ResourceLocation inputFluid, ResourceLocation outputFluid)
    implements FluidProcessRecipe {
  public static final MapCodec<StrainingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      ResourceLocation.CODEC.fieldOf("input_fluid").forGetter(StrainingRecipe::inputFluid),
      ResourceLocation.CODEC.fieldOf("output_fluid").forGetter(StrainingRecipe::outputFluid)
  ).apply(instance, StrainingRecipe::new));

  public boolean accepts(FluidStack input) {
    return !input.isEmpty() && io.vinum.beverage.WineCatalog.isWine(input) && BuiltInRegistries.FLUID.getKey(input.getFluid()).equals(inputFluid)
        && BatchProfile.of(input).solidsGramsPerLitre() > 0
        && BuiltInRegistries.FLUID.containsKey(outputFluid)
        && BuiltInRegistries.FLUID.get(outputFluid) != Fluids.EMPTY;
  }

  public FluidStack strain(FluidStack input) {
    if (!accepts(input)) return FluidStack.EMPTY;
    FluidStack output = new FluidStack(BuiltInRegistries.FLUID.get(outputFluid), input.getAmount());
    output.applyComponents(input.getComponentsPatch());
    BatchProfile profile = BatchProfile.of(input);
    return new BatchProfile(profile.sugarGramsPerLitre(), profile.starchGramsPerLitre(), 0,
        profile.alcoholFraction(), profile.ingredientProportions(), profile.yeast(), profile.history())
        .addHistory(ProductionInputs.id("straining")).applyTo(output);
  }

  @Override public RecipeSerializer<?> getSerializer() { return StrainerContent.SERIALIZER.get(); }
  @Override public RecipeType<?> getType() { return StrainerContent.TYPE.get(); }
}
