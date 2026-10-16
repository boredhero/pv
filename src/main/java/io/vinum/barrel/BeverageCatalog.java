package io.vinum.barrel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import io.vinum.common.Defines;
import io.vinum.config.BarrelConfig;
import io.vinum.fluid.ModFluids;
import io.vinum.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Extend during mod common setup with register(); registry suppliers are resolved only during gameplay. */
public final class BeverageCatalog {
    private static final List<BeverageDefinition> DEFINITIONS = new ArrayList<>();
    public static final AgingProfile TEQUILA_AGING = new AgingProfile(List.of(
            new AgingProfile.Stage(() -> 0, "tooltip.vinum.maturation.blanco"),
            new AgingProfile.Stage(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaReposadoDays()), "tooltip.vinum.maturation.reposado"),
            new AgingProfile.Stage(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaAnejoDays()), "tooltip.vinum.maturation.anejo"),
            new AgingProfile.Stage(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaExtraAnejoDays()), "tooltip.vinum.maturation.extra_anejo")));
    public static final AgingProfile WINE_AGING = new AgingProfile(List.of(
            new AgingProfile.Stage(() -> 0, "tooltip.vinum.maturation.young"),
            new AgingProfile.Stage(() -> BarrelConfig.daysToTicks(BarrelConfig.wineMatureDays()), "tooltip.vinum.maturation.mature"),
            new AgingProfile.Stage(() -> BarrelConfig.daysToTicks(BarrelConfig.wineReserveDays()), "tooltip.vinum.maturation.reserve")));
    public static final AgingProfile GOLDEN_AGING = new AgingProfile(List.of(
            new AgingProfile.Stage(() -> 0, "tooltip.vinum.maturation.golden")));

    static {
        register(new BeverageDefinition(id("tequila"), ModFluids.TEQUILA.source(), TEQUILA_AGING, List.of(
                new Serving(ModItems.FIFTH_BOTTLE_EMPTY, 750, List.of(
                        new Serving.Variant(() -> 0, ModItems.FIFTH_SILVER_TEQUILA),
                        new Serving.Variant(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaReposadoDays()), ModItems.FIFTH_REPOSADO_TEQUILA),
                        new Serving.Variant(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaAnejoDays()), ModItems.FIFTH_ANEJO_TEQUILA),
                        new Serving.Variant(() -> BarrelConfig.daysToTicks(BarrelConfig.tequilaExtraAnejoDays()), ModItems.FIFTH_EXTRA_ANEJO_TEQUILA))))));
        register(new BeverageDefinition(id("golden_tequila"), ModFluids.GOLDEN_TEQUILA.source(), GOLDEN_AGING,
                List.of(singleServing(ModItems.FIFTH_BOTTLE_EMPTY, 750, ModItems.FIFTH_GOLDEN_TEQUILA))));
        registerWine("chardonnay", ModFluids.CHARDONNAY, ModItems.WINE_BOTTLE_CHARDONNAY, ModItems.WINE_GLASS_CHARDONNAY);
        registerWine("sauvignon_blanc", ModFluids.SAUVIGNON_BLANC, ModItems.WINE_BOTTLE_SAUVIGNON_BLANC, ModItems.WINE_GLASS_SAUVIGNON_BLANC);
        registerWine("cabernet_sauvignon", ModFluids.CABERNET_SAUVIGNON, ModItems.WINE_BOTTLE_CABERNET_SAUVIGNON, ModItems.WINE_GLASS_CABERNET_SAUVIGNON);
        registerWine("merlot", ModFluids.MERLOT, ModItems.WINE_BOTTLE_MERLOT, ModItems.WINE_GLASS_MERLOT);
        registerWine("pinot_noir", ModFluids.PINOT_NOIR, ModItems.WINE_BOTTLE_PINOT_NOIR, ModItems.WINE_GLASS_PINOT_NOIR);
        registerWine("zinfandel", ModFluids.ZINFANDEL, ModItems.WINE_BOTTLE_ZINFANDEL, ModItems.WINE_GLASS_ZINFANDEL);
    }

    private BeverageCatalog() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(Defines.MODID, path); }
    public static synchronized void register(BeverageDefinition definition) {
        if (DEFINITIONS.stream().anyMatch(existing -> existing.id().equals(definition.id()))) {
            throw new IllegalArgumentException("Duplicate beverage definition: " + definition.id());
        }
        DEFINITIONS.add(definition);
    }
    private static Serving singleServing(Supplier<? extends Item> empty, int volume, Supplier<? extends Item> filled) {
        return new Serving(empty, volume, List.of(new Serving.Variant(() -> 0, filled)));
    }
    private static void registerWine(String name, ModFluids.FluidEntry fluid, Supplier<? extends Item> bottle, Supplier<? extends Item> glass) {
        register(new BeverageDefinition(id(name), fluid.source(), WINE_AGING, List.of(
                singleServing(ModItems.WINE_BOTTLE_EMPTY, 750, bottle), singleServing(ModItems.WINE_GLASS_EMPTY, 125, glass))));
    }
    public static List<BeverageDefinition> definitions() { return List.copyOf(DEFINITIONS); }
    public static Optional<BeverageDefinition> lookupFluid(Fluid fluid) {
        return DEFINITIONS.stream().filter(definition -> definition.fluid().get().isSame(fluid)).findFirst();
    }
    public static Optional<FilledServing> lookupFilled(ItemStack stack) {
        for (BeverageDefinition definition : DEFINITIONS) {
            for (Serving serving : definition.servings()) {
                if (serving.acceptsFilled(stack)) return Optional.of(new FilledServing(definition, serving));
            }
        }
        return Optional.empty();
    }
    public static boolean isEmptyServing(ItemStack stack) {
        return DEFINITIONS.stream().flatMap(definition -> definition.servings().stream())
                .anyMatch(serving -> stack.is(serving.emptyItem().get()));
    }
    public record FilledServing(BeverageDefinition beverage, Serving serving) {}
}
