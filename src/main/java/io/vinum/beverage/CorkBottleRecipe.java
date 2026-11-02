package io.vinum.beverage;

import com.mojang.serialization.MapCodec;
import io.vinum.barrel.ModDataComponents;
import io.vinum.item.FluidContainerItem;
import io.vinum.production.ProductionRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Prepares an empty bottle for automated filling without destroying existing liquid or metadata. */
public record CorkBottleRecipe(ShapelessRecipe recipe) implements CraftingRecipe {
    public static final MapCodec<CorkBottleRecipe> CODEC = RecipeSerializer.SHAPELESS_RECIPE.codec()
            .fieldOf("recipe").xmap(CorkBottleRecipe::new, CorkBottleRecipe::recipe);
    private ItemStack bottle(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (BottleCorks.isBottle(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
    @Override public boolean matches(CraftingInput input, Level level) {
        ItemStack bottle = bottle(input);
        return recipe.matches(input, level) && bottle.getItem() instanceof FluidContainerItem item
                && item.contents(bottle).isEmpty() && !BottleCorks.hasCork(bottle);
    }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = bottle(input).copyWithCount(1);
        result.set(ModDataComponents.BOTTLE_CORK.get(), true);
        return result;
    }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return recipe.getResultItem(registries); }
    @Override public boolean canCraftInDimensions(int width, int height) { return recipe.canCraftInDimensions(width, height); }
    @Override public NonNullList<Ingredient> getIngredients() { return recipe.getIngredients(); }
    @Override public CraftingBookCategory category() { return recipe.category(); }
    @Override public RecipeSerializer<?> getSerializer() { return ProductionRecipes.CORK_BOTTLE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return RecipeType.CRAFTING; }
}
