package io.vinum.item;

import io.vinum.beverage.WineCatalog;
import io.vinum.block.ModBlocks;
import io.vinum.production.ProductionDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

/** One seed item; its persistent variety selects the plant, display name and catalog color. */
public final class GrapeSeedItem extends BlockItem {
  private static final ResourceLocation DEFAULT =
      ResourceLocation.fromNamespaceAndPath("vinum", "chardonnay");

  public GrapeSeedItem(Properties properties) {
    super(
        ModBlocks.CROP_GRAPE_CHARDONNAY.get(),
        properties.component(ProductionDataComponents.GRAPE_VARIETY.get(), DEFAULT));
  }

  public static ResourceLocation variety(ItemStack stack) {
    return stack.getOrDefault(ProductionDataComponents.GRAPE_VARIETY.get(), DEFAULT);
  }

  public static ItemStack stack(ResourceLocation variety) {
    ItemStack stack = new ItemStack(ModItems.GRAPE_SEEDS.get());
    stack.set(ProductionDataComponents.GRAPE_VARIETY.get(), variety);
    return stack;
  }

  @Override
  protected BlockState getPlacementState(BlockPlaceContext context) {
    ResourceLocation variety = variety(context.getItemInHand());
    var block =
        BuiltInRegistries.BLOCK.get(
            ResourceLocation.fromNamespaceAndPath(
                variety.getNamespace(), "crop_grape_" + variety.getPath()));
    if (!(block instanceof io.vinum.block.GrapeCropBlock)) return null;
    BlockState state = block.getStateForPlacement(context);
    return state != null && canPlace(context, state) ? state : null;
  }

  @Override
  public Component getName(ItemStack stack) {
    ResourceLocation variety = variety(stack);
    return Component.translatable(
        "item.vinum.grape_seeds",
        Component.translatable("fluid." + variety.getNamespace() + "." + variety.getPath()));
  }

  public static int color(ItemStack stack) {
    ResourceLocation variety = variety(stack);
    return WineCatalog.varieties().stream()
        .filter(v -> v.legacyFluid().equals(variety))
        .map(WineCatalog.Variety::mustColor)
        .findFirst()
        .orElse(0xFF94734B);
  }
}
