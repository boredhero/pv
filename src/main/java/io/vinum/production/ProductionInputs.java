package io.vinum.production;

import io.vinum.barrel.ModDataComponents;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Small extensible adapter catalog; suppliers are resolved only after registries are ready. */
public final class ProductionInputs {
    private static final Map<ResourceLocation, ResourceLocation> ITEMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, BatchProfile> PROFILES = new LinkedHashMap<>();
    static {
        for (String variety : List.of("chardonnay", "sauvignon_blanc", "cabernet_sauvignon", "merlot", "pinot_noir", "zinfandel")) {
            register(id("grape_juice_" + variety), id("juice_" + variety), profile(id("grapes_" + variety), 200, 0));
            register(id("grape_must_" + variety), id("must_" + variety), profile(id("grapes_" + variety), 200, 100));
        }
        register(id("unfermented_agave_wort"), id("agave_mosto"), profile(id("agave"), 500.0 / 3, 0));
    }
    private ProductionInputs() {}
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("vinum", path); }
    private static BatchProfile profile(ResourceLocation ingredient, double sugar, double solids) {
        return new BatchProfile(sugar, 0, solids, 0, Map.of(ingredient, 1.0), Optional.empty(), List.of());
    }
    public static synchronized void register(ResourceLocation item, ResourceLocation fluid, BatchProfile profile) {
        if (ITEMS.putIfAbsent(item, fluid) != null) throw new IllegalArgumentException("Duplicate production input " + item);
        PROFILES.put(fluid, profile);
    }
    /** Represents one prepared item (not the complete stack); does not consume it. */
    public static FluidStack fromItem(ItemStack item) {
        if (item.isEmpty()) return FluidStack.EMPTY;
        ResourceLocation fluid = ITEMS.get(BuiltInRegistries.ITEM.getKey(item.getItem()));
        if (fluid == null || !BuiltInRegistries.FLUID.containsKey(fluid)) return FluidStack.EMPTY;
        var stored = item.get(ModDataComponents.CONTAINED_FLUID.get());
        if (stored != null) {
            FluidStack contents = stored.copy();
            return contents.getAmount() == 1000 && BuiltInRegistries.FLUID.getKey(contents.getFluid()).equals(fluid)
                    ? contents : FluidStack.EMPTY;
        }
        return defaultProfile(fluid).applyTo(new FluidStack(BuiltInRegistries.FLUID.get(fluid), 1000));
    }
    public static BatchProfile defaultProfile(FluidStack fluid) {
        return defaultProfile(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
    }
    private static BatchProfile defaultProfile(ResourceLocation fluid) {
        return PROFILES.getOrDefault(fluid, new BatchProfile(0, 0, 0, 0, Map.of(), Optional.empty(), List.of()));
    }
}
