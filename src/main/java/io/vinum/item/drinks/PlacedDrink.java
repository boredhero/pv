package io.vinum.item.drinks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A displayed serving is consumed even in Creative; the returned vessel stays on the counter. */
public interface PlacedDrink {
    ItemStack finishDrinkingOnBar(ItemStack serving, Level level, Player player);
}
