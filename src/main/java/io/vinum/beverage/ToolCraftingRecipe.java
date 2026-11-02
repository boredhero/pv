package io.vinum.beverage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.production.ProductionRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Shapeless crafting with one reusable, component-preserving tool and normal durability rules. */
public record ToolCraftingRecipe(ShapelessRecipe recipe, ResourceLocation toolItem, int toolDamage)
    implements CraftingRecipe {
  public static final MapCodec<ToolCraftingRecipe> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      RecipeSerializer.SHAPELESS_RECIPE
                          .codec()
                          .codec()
                          .fieldOf("recipe")
                          .forGetter(ToolCraftingRecipe::recipe),
                      ResourceLocation.CODEC
                          .fieldOf("tool_item")
                          .forGetter(ToolCraftingRecipe::toolItem),
                      Codec.intRange(1, 1024)
                          .optionalFieldOf("tool_damage", 1)
                          .forGetter(ToolCraftingRecipe::toolDamage))
                  .apply(i, ToolCraftingRecipe::new));

  @Override
  public boolean matches(CraftingInput input, Level level) {
    return recipe.matches(input, level) && input.items().stream().anyMatch(this::isTool);
  }

  private boolean isTool(ItemStack stack) {
    return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(toolItem);
  }

  @Override
  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    var result = recipe.getRemainingItems(input);
    var player = CommonHooks.getCraftingPlayer();
    var server = ServerLifecycleHooks.getCurrentServer();
    // Recipe remainders have no Level argument. Player crafting uses its actual level;
    // vanilla automatic crafters use the server's overworld for enchantment durability RNG.
    var level =
        player instanceof ServerPlayer sp
            ? sp.serverLevel()
            : server == null ? null : server.overworld();
    for (int slot = 0; slot < input.size(); slot++) {
      if (!isTool(input.getItem(slot))) continue;
      ItemStack tool = input.getItem(slot).copyWithCount(1);
      if (level != null)
        tool.hurtAndBreak(
            toolDamage, level, player instanceof ServerPlayer sp ? sp : null, ignored -> {});
      else if (tool.isDamageableItem()) {
        // Client-only prediction; authoritative remainder comes from the server.
        tool.setDamageValue(tool.getDamageValue() + toolDamage);
        if (tool.getDamageValue() >= tool.getMaxDamage()) tool = ItemStack.EMPTY;
      }
      result.set(slot, tool);
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
    return ProductionRecipes.TOOL_CRAFTING_SERIALIZER.get();
  }

  @Override
  public RecipeType<?> getType() {
    return RecipeType.CRAFTING;
  }
}
