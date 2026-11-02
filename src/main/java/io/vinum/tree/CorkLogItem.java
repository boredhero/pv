package io.vinum.tree;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

/** Harvested logs carry their deadline; breaking and replacing never resets the bark cycle. */
public final class CorkLogItem extends BlockItem {
  public CorkLogItem(Block block, Properties properties) {
    super(block, properties);
  }

  /** The same saved deadline drives placement, tooltip and inventory artwork. */
  public static boolean harvested(ItemStack stack) {
    var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
    return data != null
        && data.copyTag().contains("CorkReadyAt")
        && data.copyTag().getLong("CorkReadyAt") >= 0;
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return stack.has(DataComponents.BLOCK_ENTITY_DATA) ? 1 : super.getMaxStackSize(stack);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    tooltip.add(
        Component.translatable(
            harvested(stack) ? "tooltip.vinum.cork_log_used" : "tooltip.vinum.cork_log"));
  }
}
