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

    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.machine.WinePressMenu>> WINE_PRESS = CONTAINERS.register("wine_press", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(io.vinum.machine.WinePressMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.machine.BasicGeneratorMenu>> BASIC_GENERATOR = CONTAINERS.register("basic_generator", () -> new MenuType<>(io.vinum.machine.BasicGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.sugar.SugarPlantMenu>> SUGAR_PLANT =
            CONTAINERS.register("sugar_plant", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(io.vinum.sugar.SugarPlantMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<io.vinum.blender.BlenderMenu>> BLENDER =
            CONTAINERS.register("blender", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(io.vinum.blender.BlenderMenu::new));

    private ModContainers() {}
}
