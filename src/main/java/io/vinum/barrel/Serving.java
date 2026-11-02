package io.vinum.barrel;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A container volume and its filled variants, optionally selected by maturation thresholds. */
public record Serving(Supplier<? extends Item> emptyItem, int volume, List<Variant> variants) {
    public Serving {
        variants = List.copyOf(variants);
        if (volume <= 0 || variants.isEmpty()) throw new IllegalArgumentException("Serving requires positive volume and a filled variant");
    }
    public record Variant(LongSupplier minimumTicks, Supplier<? extends Item> item) {}

    public Item filledItem(long ageTicks) {
        Item selected = variants.getFirst().item().get();
        long minimum = -1;
        for (Variant variant : variants) {
            long threshold = variant.minimumTicks().getAsLong();
            if (ageTicks >= threshold && threshold >= minimum) {
                selected = variant.item().get();
                minimum = threshold;
            }
        }
        return selected;
    }
    public boolean acceptsFilled(ItemStack stack) { return variants.stream().anyMatch(variant -> stack.is(variant.item().get())); }
    public long age(ItemStack stack) {
        Long explicit = stack.get(ModDataComponents.FLUID_AGE.get());
        if (explicit != null) return Math.max(0L, explicit);
        return variants.stream().filter(variant -> stack.is(variant.item().get()))
                .mapToLong(variant -> variant.minimumTicks().getAsLong()).findFirst().orElse(0);
    }
    public ItemStack createFilled(long ageTicks) {
        ItemStack stack = new ItemStack(filledItem(ageTicks));
        // Fresh servings remain stack-compatible with recipe/creative-tab bottles.
        if (ageTicks > 0) stack.set(ModDataComponents.FLUID_AGE.get(), ageTicks);
        return stack;
    }
}
