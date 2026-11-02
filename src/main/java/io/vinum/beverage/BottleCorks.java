package io.vinum.beverage;

import io.vinum.barrel.ModDataComponents;
import io.vinum.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** A supplied cork belongs to one filling, not to the liquid; fluid transfers never copy it. */
public final class BottleCorks {
    public static final TagKey<Item> BOTTLES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("vinum", "corkable_bottles"));
    private BottleCorks() {}
    public static boolean isBottle(ItemStack stack) { return stack.is(BOTTLES); }
    public static boolean hasCork(ItemStack stack) { return stack.getOrDefault(ModDataComponents.BOTTLE_CORK.get(), false); }
    public static boolean requiresCork(ItemStack stack, FluidStack incoming) {
        return isBottle(stack) && WineCatalog.isWine(incoming) && !hasCork(stack);
    }
    public static ItemStack findCork(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item.is(ModItems.WINE_CORK.get())) return item;
        }
        return ItemStack.EMPTY;
    }
    /** Called on the transaction's copy only; charge after successful commit. */
    public static boolean prepare(ItemStack copy, FluidStack incoming, Player player) {
        if (!requiresCork(copy, incoming)) return true;
        if (!player.hasInfiniteMaterials() && findCork(player).isEmpty()) {
            if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("message.vinum.cork_required"), true);
            return false;
        }
        copy.set(ModDataComponents.BOTTLE_CORK.get(), true);
        return true;
    }
    public static void consume(Player player) {
        if (!player.hasInfiniteMaterials()) findCork(player).shrink(1);
    }
}
