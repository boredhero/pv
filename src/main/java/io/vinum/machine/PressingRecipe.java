package io.vinum.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.production.ProductionRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Datapack-defined input and exact fluid yield; both crank and FE pay the same work cost. */
public record PressingRecipe(Ingredient ingredient, int count, FluidStack output, int energy, ItemStack byproduct)
    implements Recipe<SingleRecipeInput> {
  public static final MapCodec<PressingRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      Ingredient.CODEC.fieldOf("ingredient").forGetter(PressingRecipe::ingredient),
                      Codec.intRange(1, 64).fieldOf("count").forGetter(PressingRecipe::count),
                      FluidStack.CODEC.fieldOf("output").forGetter(PressingRecipe::output),
                      Codec.intRange(1, 32000).fieldOf("energy").forGetter(PressingRecipe::energy),
                      ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(PressingRecipe::byproduct))
                  .apply(i, PressingRecipe::new));

  public PressingRecipe(Ingredient ingredient, int count, FluidStack output, int energy) { this(ingredient,count,output,energy,ItemStack.EMPTY); }

  public PressingRecipe {
    if (ingredient.isEmpty()
        || count < 1
        || count > 64
        || output.isEmpty()
        || output.getAmount() > WinePressBlockEntity.TANK_CAPACITY
        || energy < 1
        || energy > 32000) throw new IllegalArgumentException("Invalid pressing recipe");
    output = output.copy();
    byproduct = byproduct.copy();
  }

  @Override public ItemStack byproduct() { return byproduct.copy(); }

  @Override
  public FluidStack output() {
    return output.copy();
  }

  public boolean accepts(ItemStack stack) {
    return ingredient.test(stack);
  }

  public boolean canProcess(ItemStack stack) {
    return accepts(stack) && stack.getCount() >= count;
  }

  @Override
  public boolean matches(SingleRecipeInput input, Level level) {
    return canProcess(input.item());
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return false;
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean isSpecial() {
    return true;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ProductionRecipes.PRESSING_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return ProductionRecipes.PRESSING_TYPE.get();
  }
}
