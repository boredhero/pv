package io.vinum.beverage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.item.FluidContainerItem;
import io.vinum.production.ProductionRecipes;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Standard shaped crafting plus a tag/volume requirement on reusable fluid servings. */
public record FluidCraftingRecipe(
    ShapedRecipe recipe, ResourceLocation containerItem, ResourceLocation fluidTag, int amount)
    implements CraftingRecipe {
  public static final MapCodec<FluidCraftingRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      RecipeSerializer.SHAPED_RECIPE
                          .codec()
                          .codec()
                          .fieldOf("recipe")
                          .forGetter(FluidCraftingRecipe::recipe),
                      ResourceLocation.CODEC
                          .fieldOf("container_item")
                          .forGetter(FluidCraftingRecipe::containerItem),
                      ResourceLocation.CODEC
                          .fieldOf("fluid_tag")
                          .forGetter(FluidCraftingRecipe::fluidTag),
                      Codec.intRange(1, 16000)
                          .fieldOf("amount")
                          .forGetter(FluidCraftingRecipe::amount))
                  .apply(i, FluidCraftingRecipe::new));

  @Override
  public boolean matches(CraftingInput input, Level level) {
    if (!recipe.matches(input, level)) return false;
    boolean found = false;
    for (int slot = 0; slot < input.size(); slot++) {
      ItemStack stack = input.getItem(slot);
      if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(containerItem)) continue;
      found = true;
      if (!(stack.getItem() instanceof FluidContainerItem container)) return false;
      var contents = container.contents(stack);
      if (contents.getAmount() < amount || !contents.is(TagKey.create(Registries.FLUID, fluidTag)))
        return false;
    }
    return found;
  }

  @Override
  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    var result = recipe.getRemainingItems(input);
    for (int slot = 0; slot < input.size(); slot++) {
      ItemStack stack = input.getItem(slot);
      if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(containerItem)
          && stack.getItem() instanceof FluidContainerItem container) {
        var handler = container.handler(stack.copyWithCount(1));
        handler.drain(amount, FluidAction.EXECUTE);
        result.set(slot, handler.getContainer());
      }
    }
    return result;
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    return recipe.assemble(input, registries);
  }

  @Override
  public ItemStack getResultItem(HolderLookup.Provider registries) {
    return recipe.getResultItem(registries);
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return recipe.canCraftInDimensions(width, height);
  }

  @Override
  public NonNullList<Ingredient> getIngredients() {
    return recipe.getIngredients();
  }

  @Override
  public String getGroup() {
    return recipe.getGroup();
  }

  @Override
  public CraftingBookCategory category() {
    return recipe.category();
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ProductionRecipes.FLUID_CRAFTING_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return RecipeType.CRAFTING;
  }
}
