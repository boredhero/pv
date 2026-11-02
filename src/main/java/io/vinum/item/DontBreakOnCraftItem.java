package io.vinum.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Tools and barrels survive crafting without duplicating a whole input stack. */
public class DontBreakOnCraftItem extends Item {
    public DontBreakOnCraftItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }
}
