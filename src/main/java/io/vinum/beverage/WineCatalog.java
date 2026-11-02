package io.vinum.beverage;

import io.vinum.fluid.ModFluids;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionDataComponents;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Wine varieties are ingredient metadata, not fluid identities. Legacy fluids are read adapters only. */
public final class WineCatalog {
    public record Variety(ResourceLocation ingredient, ResourceLocation legacyFluid, int color, int juiceColor, int mustColor, int grapeColor, double defaultAlcohol) {
        public Variety(ResourceLocation ingredient, ResourceLocation legacyFluid, int color, int juiceColor, int mustColor, double defaultAlcohol) {
            this(ingredient, legacyFluid, color, juiceColor, mustColor, mustColor, defaultAlcohol);
        }
    }
    private static final Map<ResourceLocation, Variety> VARIETIES = new LinkedHashMap<>();
    static {
        try (var input = WineCatalog.class.getResourceAsStream("/vinum/wines.json")) {
            if (input == null) throw new IllegalStateException("Missing wine definitions");
            var entries = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            for (var entry : entries) {
                var data = entry.getAsJsonObject();
                register(new Variety(ResourceLocation.parse(data.get("ingredient").getAsString()),
                        ResourceLocation.fromNamespaceAndPath("vinum", data.get("id").getAsString()), hex(data, "wine_color"), hex(data, "juice_color"),
                        hex(data, "must_color"), hex(data, data.has("grape_color") ? "grape_color" : "must_color"), data.get("default_alcohol").getAsDouble()));
            }
        } catch (java.io.IOException e) { throw new ExceptionInInitializerError(e); }
    }
    private WineCatalog() {}
    private static int hex(com.google.gson.JsonObject data, String key) {
        String value = data.get(key).getAsString();
        if (!value.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Expected six-digit RGB: " + key);
        return 0xFF000000 | Integer.parseInt(value, 16);
    }
    public static synchronized void register(Variety variety) {
        if (!Double.isFinite(variety.defaultAlcohol()) || variety.defaultAlcohol() < 0 || variety.defaultAlcohol() > 1)
            throw new IllegalArgumentException("Invalid default alcohol fraction");
        if (VARIETIES.putIfAbsent(variety.ingredient(), variety) != null) throw new IllegalArgumentException("Duplicate wine ingredient");
    }
    public static List<Variety> varieties() { return List.copyOf(VARIETIES.values()); }
    public static boolean isWine(FluidStack fluid) {
        if (fluid.isEmpty()) return false;
        return fluid.getFluid().isSame(ModFluids.WINE.source().get()) || legacy(fluid).isPresent();
    }
    private static Optional<Variety> legacy(FluidStack fluid) {
        var id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        return VARIETIES.values().stream().filter(v -> v.legacyFluid().equals(id)
                || id.equals(ResourceLocation.fromNamespaceAndPath(v.legacyFluid().getNamespace(), "flowing_" + v.legacyFluid().getPath()))).findFirst();
    }
    /** Copy, preserving unknown components and explicit profiles, including explicit zero alcohol. */
    public static FluidStack normalize(FluidStack input) {
        if (input.isEmpty()) return FluidStack.EMPTY;
        var old = legacy(input);
        if (old.isEmpty()) return input.copy();
        FluidStack result = new FluidStack(ModFluids.WINE.source().get(), input.getAmount());
        result.applyComponents(input.getComponentsPatch());
        if (!input.has(ProductionDataComponents.BATCH_PROFILE.get())) {
            new BatchProfile(0, 0, 0, old.get().defaultAlcohol(), Map.of(old.get().ingredient(), 1D),
                    Optional.empty(), List.of()).applyTo(result);
        }
        return result;
    }
    public static FluidStack sample(Variety variety, int amount) {
        return new BatchProfile(0, 0, 0, variety.defaultAlcohol(), Map.of(variety.ingredient(), 1D), Optional.empty(), List.of())
                .applyTo(new FluidStack(ModFluids.WINE.source().get(), amount));
    }
    public static Component name(FluidStack fluid) {
        if (!isWine(fluid)) return BeverageAppearance.find(fluid).<Component>map(style -> style.name().isBlank() ? fluid.getHoverName() : Component.translatable(style.name())).orElseGet(fluid::getHoverName);
        var ingredients = BatchProfile.of(normalize(fluid)).ingredientProportions();
        if (ingredients.size() == 1) {
            var variety = VARIETIES.get(ingredients.keySet().iterator().next());
            if (variety != null) return Component.translatable("fluid." + variety.legacyFluid().getNamespace() + "." + variety.legacyFluid().getPath());
        }
        return Component.translatable(ingredients.size() > 1 ? "fluid.vinum.wine_blend" : "fluid.vinum.wine");
    }
    public static int color(FluidStack fluid) {
        if (fluid.isEmpty()) return 0xFFFFFFFF;
        var appearance=BeverageAppearance.find(fluid);
        if(appearance.isPresent())return appearance.get().liquidColor();
        var ingredients = BatchProfile.of(normalize(fluid)).ingredientProportions();
        boolean wine = isWine(fluid);
        boolean must = BatchProfile.of(fluid).solidsGramsPerLitre() > 0;
        if (ingredients.isEmpty()) return ModFluids.entries().stream().filter(e -> e.source().get().isSame(fluid.getFluid()))
                .map(ModFluids.FluidEntry::color).findFirst().orElse(0xFFB7D9EC);
        double r = 0, g = 0, b = 0;
        if (ingredients.isEmpty()) return 0xFF842D40;
        for (var entry : ingredients.entrySet()) {
            var variety = VARIETIES.get(entry.getKey());
            int color = variety == null ? 0xFF842D40 : wine ? variety.color() : must ? variety.mustColor() : variety.juiceColor();
            r += (color >> 16 & 255) * entry.getValue(); g += (color >> 8 & 255) * entry.getValue(); b += (color & 255) * entry.getValue();
        }
        return 0xFF000000 | ((int)Math.round(r) << 16) | ((int)Math.round(g) << 8) | (int)Math.round(b);
    }
}
