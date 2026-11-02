package io.vinum.machine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

public final class MachineBlockItem extends BlockItem {
  public MachineBlockItem(Block block, Properties properties) {
    super(block, properties);
  }

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return stack.has(DataComponents.BLOCK_ENTITY_DATA) ? 1 : super.getMaxStackSize(stack);
  }
}
