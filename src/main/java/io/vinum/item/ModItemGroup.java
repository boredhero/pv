package io.vinum.item;

import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItemGroup {
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Defines.MODID);
  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
      TABS.register(
          "main",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("itemGroup.vinum"))
                  .icon(() -> ModItems.FIFTH_BOTTLE_EMPTY.get().getDefaultInstance())
                  .displayItems(
                      (parameters, output) -> {
                        ModItems.ITEMS
                            .getEntries()
                            .forEach(
                                item -> {
                                  // Legacy prepared bowls remain loadable, but pressing owns must production.
                                  if (item.getId().getPath().startsWith("grape_must_")) return;
                                  if (item.get() == ModItems.FIFTH_BOTTLE_EMPTY.get()
                                      || item.get() == ModItems.SHOT_GLASS.get()) {
                                    output.accept(item.get());
                                    var container = (FluidContainerItem) item.get();
                                    for (int days :
                                        new int[] {
                                          0,
                                          io.vinum.config.BarrelConfig.tequilaReposadoDays(),
                                          io.vinum.config.BarrelConfig.tequilaAnejoDays(),
                                          io.vinum.config.BarrelConfig.tequilaExtraAnejoDays()
                                        })
                                      output.accept(
                                          container.filled(
                                              io.vinum.barrel.BarrelFluidData.withAge(
                                                  ModItems.spirit(
                                                      io.vinum.fluid.ModFluids.TEQUILA
                                                          .source()
                                                          .get(),
                                                      container.capacity()),
                                                  io.vinum.config.BarrelConfig.daysToTicks(days))));
                                    if (item.get() == ModItems.SHOT_GLASS.get()) addRumExamples(output, container);
                                    return;
                                  }
                                  if (item.get() == ModItems.RUM_BOTTLE.get()) {
                                    output.accept(item.get());
                                    var container = (FluidContainerItem) item.get();
                                    addRumExamples(output, container);
                                    return;
                                  }
                                  if (item.get() == ModItems.WINE_BOTTLE_EMPTY.get()
                                      || item.get() == ModItems.WINE_GLASS_EMPTY.get()) {
                                    output.accept(item.get());
                                    var bottle = (FluidContainerItem) item.get();
                                    for (var variety : io.vinum.beverage.WineCatalog.varieties()) {
                                      var wine = io.vinum.beverage.WineCatalog.sample(variety, bottle.capacity());
                                      output.accept(bottle.filled(io.vinum.barrel.BarrelFluidData.withAge(wine,
                                          io.vinum.config.BarrelConfig.daysToTicks(io.vinum.config.BarrelConfig.wineMatureDays()))));
                                    }
                                    return;
                                  }
                                  if (item.get() instanceof GrapeSeedItem) {
                                    io.vinum.beverage.WineCatalog.varieties()
                                        .forEach(
                                            variety ->
                                                output.accept(
                                                    GrapeSeedItem.stack(variety.legacyFluid())));
                                    return;
                                  }
                                  if (item.get() == ModItems.UNFERMENTED_AGAVE_WORT.get()
                                      || item.get() == ModItems.FERMENTED_AGAVE_WORT.get()
                                      || item.get() == ModItems.BATCH_BUCKET.get()) return;
                                  // Keep legacy registry IDs loadable without presenting eight
                                  // identical barrels.
                                  if (item.get() instanceof FluidContainerItem container
                                      && container.legacy()) return;
                                  if (!(item.get() instanceof BarrelItem)
                                      || item.get() == ModItems.BARREL.get()) {
                                    output.accept(item.get());
                                  }
                                });
                        io.vinum.tree.CorkOakContent.ITEMS
                            .getEntries()
                            .forEach(item -> output.accept(item.get()));
                        io.vinum.shelf.ShelfContent.ITEMS
                            .getEntries()
                            .forEach(item -> output.accept(item.get()));
                      })
                  .build());

  private static void addRumExamples(CreativeModeTab.Output output, FluidContainerItem container) {
    for (int days : new int[] {0, io.vinum.config.BarrelConfig.rumLightAgedDays(), io.vinum.config.BarrelConfig.rumDarkAgedDays()}) {
      var fluid = new net.neoforged.neoforge.fluids.FluidStack(io.vinum.fluid.ModFluids.RUM.source().get(), container.capacity());
      io.vinum.production.BatchProfile.of(fluid).withAlcoholFraction(.4).applyTo(fluid);
      output.accept(container.filled(io.vinum.barrel.BarrelFluidData.withAge(fluid,
          io.vinum.config.BarrelConfig.daysToTicks(days))));
    }
  }

  private ModItemGroup() {}
}
