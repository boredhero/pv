package io.vinum.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.barrel.ModDataComponents;
import io.vinum.item.FluidContainerItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Alcohol conservation determines wash volume; fractional mB requirements round up. */
public record DistillationRecipe(
    ResourceLocation inputFluid,
    ResourceLocation outputFluid,
    ResourceLocation outputItem,
    int durationTicks,
    double targetAlcoholFraction,
    int outputMillibuckets,
    boolean rectification)
    implements FluidProcessRecipe {
  public static final MapCodec<DistillationRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      ResourceLocation.CODEC
                          .fieldOf("input_fluid")
                          .forGetter(DistillationRecipe::inputFluid),
                      ResourceLocation.CODEC
                          .fieldOf("output_fluid")
                          .forGetter(DistillationRecipe::outputFluid),
                      ResourceLocation.CODEC
                          .fieldOf("output_item")
                          .forGetter(DistillationRecipe::outputItem),
                      Codec.intRange(1, Integer.MAX_VALUE)
                          .fieldOf("duration_ticks")
                          .forGetter(DistillationRecipe::durationTicks),
                      Codec.doubleRange(0.000001, 1)
                          .fieldOf("target_alcohol_fraction")
                          .forGetter(DistillationRecipe::targetAlcoholFraction),
                      Codec.intRange(1, 16000)
                          .fieldOf("output_millibuckets")
                          .forGetter(DistillationRecipe::outputMillibuckets),
                      Codec.BOOL.optionalFieldOf("rectification", false)
                          .forGetter(DistillationRecipe::rectification))
                  .apply(instance, DistillationRecipe::new));

  /** Existing fixed-strength recipes retain their original schema and construction API. */
  public DistillationRecipe(ResourceLocation inputFluid, ResourceLocation outputFluid,
      ResourceLocation outputItem, int durationTicks, double targetAlcoholFraction, int outputMillibuckets) {
    this(inputFluid, outputFluid, outputItem, durationTicks, targetAlcoholFraction, outputMillibuckets, false);
  }

  public DistillationRecipe {
    if (durationTicks <= 0
        || !Double.isFinite(targetAlcoholFraction)
        || targetAlcoholFraction <= 0
        || targetAlcoholFraction > 1
        || outputMillibuckets <= 0
        || outputMillibuckets > 16000)
      throw new IllegalArgumentException("Invalid distillation recipe");
  }

  /** A rectification recipe advances to the next gameplay strength, never past 95%. */
  public double targetAlcoholFraction(FluidStack input) {
    return rectification ? NeutralSpiritProcess.nextStrength(BatchProfile.of(input).alcoholFraction())
        : targetAlcoholFraction;
  }

  /** Integer.MAX_VALUE means an input with no usable alcohol or no stronger stage. */
  public int requiredInputAmount(FluidStack input) {
    double abv = BatchProfile.of(input).alcoholFraction();
    double target = targetAlcoholFraction(input);
    if (abv <= 0 || target <= 0 || abv > target) return Integer.MAX_VALUE;
    double required = outputMillibuckets * target / abv;
    return required >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.ceil(required - 1.0e-9);
  }

  public boolean matches(FluidStack input) {
    return !input.isEmpty()
        && BuiltInRegistries.FLUID.getKey(input.getFluid()).equals(inputFluid)
        && BuiltInRegistries.FLUID.containsKey(outputFluid)
        && BuiltInRegistries.ITEM.containsKey(outputItem)
        && BuiltInRegistries.FLUID.get(outputFluid) != Fluids.EMPTY
        && BuiltInRegistries.ITEM.get(outputItem) != Items.AIR
        && validOutputContainer()
        && BatchProfile.of(input).solidsGramsPerLitre() <= 0
        && requiredInputAmount(input) <= input.getAmount();
  }

  /** Reject impossible datapack outputs before consuming wash, heat, or an empty container. */
  public boolean validOutputContainer() {
    if (!BuiltInRegistries.FLUID.containsKey(outputFluid)
        || !BuiltInRegistries.ITEM.containsKey(outputItem)) return false;
    if (!(BuiltInRegistries.ITEM.get(outputItem) instanceof FluidContainerItem container)
        || container.legacy()) return false;
    FluidStack sample =
        new FluidStack(BuiltInRegistries.FLUID.get(outputFluid), outputMillibuckets);
    return !sample.isEmpty()
        && container.handler(new ItemStack(container)).fill(sample, FluidAction.SIMULATE)
            == outputMillibuckets;
  }

  public boolean acceptsContainer(ItemStack stack) {
    return validOutputContainer()
        && stack.getCount() == 1
        && stack.is(BuiltInRegistries.ITEM.get(outputItem))
        && ((FluidContainerItem) stack.getItem()).contents(stack).isEmpty();
  }

  public ItemStack output(FluidStack input) {
    if (!matches(input)) return ItemStack.EMPTY;
    FluidStack distillate =
        new FluidStack(BuiltInRegistries.FLUID.get(outputFluid), outputMillibuckets);
    distillate.applyComponents(input.getComponentsPatch());
    distillate.remove(ModDataComponents.FLUID_AGE.get());
    BatchProfile.of(input)
        .distilled(targetAlcoholFraction(input), rectification
            ? NeutralSpiritProcess.historyEvent(targetAlcoholFraction(input)) : ProductionInputs.id("distillation"))
        .applyTo(distillate);
    return ((FluidContainerItem) BuiltInRegistries.ITEM.get(outputItem)).filled(distillate);
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ProductionRecipes.DISTILLATION_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return ProductionRecipes.DISTILLATION_TYPE.get();
  }
}
