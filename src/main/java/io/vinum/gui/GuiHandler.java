package io.vinum.gui;

import io.vinum.common.Defines;
import io.vinum.gui.screen.inventory.StillMasterScreen;
import io.vinum.inventory.container.ModContainers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Defines.MODID, value = Dist.CLIENT)
public final class GuiHandler {
    private GuiHandler() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainers.STILL_MASTER.get(), StillMasterScreen::new);
        event.register(ModContainers.SUGAR_PLANT.get(), io.vinum.sugar.SugarPlantScreen::new);
        event.register(ModContainers.WINE_PRESS.get(), io.vinum.machine.WinePressScreen::new);
        event.register(ModContainers.BASIC_GENERATOR.get(), io.vinum.machine.BasicGeneratorScreen::new);
    }
}
