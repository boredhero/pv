package io.vinum.inventory.container;

import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(Registries.MENU, Defines.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<StillMasterContainer>> STILL_MASTER =
            CONTAINERS.register("still_master", () -> new MenuType<>(StillMasterContainer::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.machine.WinePressMenu>> WINE_PRESS = CONTAINERS.register("wine_press", () -> new MenuType<>(io.vinum.machine.WinePressMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.machine.BasicGeneratorMenu>> BASIC_GENERATOR = CONTAINERS.register("basic_generator", () -> new MenuType<>(io.vinum.machine.BasicGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModContainers() {}
}
