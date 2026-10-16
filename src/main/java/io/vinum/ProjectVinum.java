package io.vinum;

import io.vinum.common.Defines;
import io.vinum.core.RegistryHandler;
import io.vinum.network.NetworkLoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Defines.MODID)
public final class ProjectVinum {
    public ProjectVinum(IEventBus modBus) {
        RegistryHandler.registerDeferred(modBus);
        modBus.addListener(NetworkLoader::register);
    }
}
