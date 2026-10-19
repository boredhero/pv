package io.vinum.production;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** Fluid recipes are selected by their vessel, never by the crafting grid. */
public interface FluidProcessRecipe extends Recipe<SingleRecipeInput> {
    @Override default boolean matches(SingleRecipeInput input, Level level) { return false; }
    @Override default ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override default boolean canCraftInDimensions(int width, int height) { return false; }
    @Override default ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override default boolean isSpecial() { return true; }
    @Override default boolean isIncomplete() { return false; }
}
