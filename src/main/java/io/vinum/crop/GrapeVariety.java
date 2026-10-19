package io.vinum.crop;

import com.mojang.serialization.Codec;
import io.vinum.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/** Stable variety names shared by grape plants, their seeds and harvested fruit. */
public enum GrapeVariety implements StringRepresentable {
    CHARDONNAY("chardonnay", () -> ModItems.GRAPE_SEEDS_CHARDONNAY.get(), () -> ModItems.GRAPES_CHARDONNAY.get()),
    SAUVIGNON_BLANC("sauvignon_blanc", () -> ModItems.GRAPE_SEEDS_SAUVIGNON_BLANC.get(), () -> ModItems.GRAPES_SAUVIGNON_BLANC.get()),
    CABERNET_SAUVIGNON("cabernet_sauvignon", () -> ModItems.GRAPE_SEEDS_CABERNET_SAUVIGNON.get(), () -> ModItems.GRAPES_CABERNET_SAUVIGNON.get()),
    MERLOT("merlot", () -> ModItems.GRAPE_SEEDS_MERLOT.get(), () -> ModItems.GRAPES_MERLOT.get()),
    PINOT_NOIR("pinot_noir", () -> ModItems.GRAPE_SEEDS_PINOT_NOIR.get(), () -> ModItems.GRAPES_PINOT_NOIR.get()),
    ZINFANDEL("zinfandel", () -> ModItems.GRAPE_SEEDS_ZINFANDEL.get(), () -> ModItems.GRAPES_ZINFANDEL.get());

    public static final Codec<GrapeVariety> CODEC = StringRepresentable.fromEnum(GrapeVariety::values);
    private final String name;
    private final Supplier<? extends Item> seeds;
    private final Supplier<? extends Item> grapes;

    GrapeVariety(String name, Supplier<? extends Item> seeds, Supplier<? extends Item> grapes) {
        this.name = name;
        this.seeds = seeds;
        this.grapes = grapes;
    }

    @Override
    public String getSerializedName() { return name; }
    public Item seedItem() { return seeds.get(); }
    public Item grapeItem() { return grapes.get(); }
}
