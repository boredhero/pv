package io.vinum.crop;

import com.mojang.serialization.Codec;
import io.vinum.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/** Stable variety names shared by grape plants, their seeds and harvested fruit. */
public enum GrapeVariety implements StringRepresentable {
    CHARDONNAY("chardonnay", () -> ModItems.GRAPES_CHARDONNAY.get()),
    SAUVIGNON_BLANC("sauvignon_blanc", () -> ModItems.GRAPES_SAUVIGNON_BLANC.get()),
    CABERNET_SAUVIGNON("cabernet_sauvignon", () -> ModItems.GRAPES_CABERNET_SAUVIGNON.get()),
    MERLOT("merlot", () -> ModItems.GRAPES_MERLOT.get()),
    PINOT_NOIR("pinot_noir", () -> ModItems.GRAPES_PINOT_NOIR.get()),
    ZINFANDEL("zinfandel", () -> ModItems.GRAPES_ZINFANDEL.get());

    public static final Codec<GrapeVariety> CODEC = StringRepresentable.fromEnum(GrapeVariety::values);
    private final String name;
    private final Supplier<? extends Item> grapes;

    GrapeVariety(String name, Supplier<? extends Item> grapes) {
        this.name = name;
        this.grapes = grapes;
    }

    @Override
    public String getSerializedName() { return name; }
    public Item seedItem() { return ModItems.GRAPE_SEEDS.get(); }
    public net.minecraft.world.item.ItemStack seeds() {
        return io.vinum.item.GrapeSeedItem.stack(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("vinum",name));
    }
    public Item grapeItem() { return grapes.get(); }
}
