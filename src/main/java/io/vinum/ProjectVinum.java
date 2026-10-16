package io.vinum;

import io.vinum.common.Defines;
import io.vinum.config.BarrelConfig;
import io.vinum.core.RegistryHandler;
import io.vinum.network.NetworkLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(Defines.MODID)
public final class ProjectVinum {
    public ProjectVinum(IEventBus modBus, ModContainer container) {
        RegistryHandler.registerDeferred(modBus);
        modBus.addListener(NetworkLoader::register);
        container.registerConfig(ModConfig.Type.SERVER, BarrelConfig.SPEC);
    }
}
