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
        io.vinum.tree.CorkOakContent.register(modBus, container);
        io.vinum.straining.StrainerContent.register(modBus);
        io.vinum.shelf.ShelfContent.register(modBus);
        io.vinum.bar.BarContent.register(modBus);
        io.vinum.stool.StoolContent.register(modBus);
        io.vinum.sugar.SugarContent.register(modBus);
        io.vinum.cinnamon.CinnamonContent.register(modBus);
        io.vinum.grain.RyeCornContent.register(modBus);
        io.vinum.worldgen.AgaveWorldgen.register(modBus);
        modBus.addListener(NetworkLoader::register);
        container.registerConfig(ModConfig.Type.SERVER, BarrelConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, io.vinum.crop.VineyardConfig.SPEC, "vinum-vineyard-server.toml");
    }
}
