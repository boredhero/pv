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
        // One variety catalog owns ingredient identity; defaults must not silently omit new varieties.
        for (var variety : io.vinum.beverage.WineCatalog.varieties()) {
            ResourceLocation key = variety.legacyFluid();
            register(related(key, "grape_juice_"), related(key, "juice_"), profile(variety.ingredient(), 200, 0));
            register(related(key, "grape_must_"), related(key, "must_"), profile(variety.ingredient(), 200, 100));
        }
        loadFluidDefaults();
        register(id("unfermented_agave_wort"), id("agave_mosto"), profile(id("agave"), 500.0 / 3, 0));
    }
    /** Packaged adapters for unprofiled fluids; explicit batch components always take precedence. */
    private static void loadFluidDefaults() {
        try (var stream = ProductionInputs.class.getResourceAsStream("/vinum/production_fluid_defaults.json")) {
            if (stream == null) throw new IllegalStateException("Missing production fluid defaults");
            var entries = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(
                    stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : entries.entrySet()) {
                var value = entry.getValue().getAsJsonObject();
                registerFluidProfile(ResourceLocation.parse(entry.getKey()), new BatchProfile(
                        number(value, "sugar_g_per_litre"), number(value, "starch_g_per_litre"),
                        number(value, "solids_g_per_litre"), number(value, "alcohol_fraction"),
                        Map.of(ResourceLocation.parse(value.get("ingredient").getAsString()), 1D),
                        Optional.empty(), List.of()));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read production fluid defaults", e);
        }
    }
    private static double number(com.google.gson.JsonObject value, String key) {
        return value.has(key) ? value.get(key).getAsDouble() : 0;
    }
    private ProductionInputs() {}
    private static ResourceLocation related(ResourceLocation variety, String prefix) {
        return ResourceLocation.fromNamespaceAndPath(variety.getNamespace(), prefix + variety.getPath());
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("vinum", path); }
    private static BatchProfile profile(ResourceLocation ingredient, double sugar, double solids) {
        return new BatchProfile(sugar, 0, solids, 0, Map.of(ingredient, 1.0), Optional.empty(), List.of());
    }
    public static synchronized void register(ResourceLocation item, ResourceLocation fluid, BatchProfile profile) {
        if (ITEMS.putIfAbsent(item, fluid) != null) throw new IllegalArgumentException("Duplicate production input " + item);
        PROFILES.put(fluid, profile);
    }
    /** Defaults for tank-only fluids; no fabricated prepared item is needed. */
    public static synchronized void registerFluidProfile(ResourceLocation fluid, BatchProfile profile) {
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
