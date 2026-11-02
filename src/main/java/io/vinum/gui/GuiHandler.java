package io.vinum.gui;

import io.vinum.common.Defines;
import io.vinum.gui.screen.inventory.StillMasterScreen;
import io.vinum.inventory.container.ModContainers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Defines.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GuiHandler {
    private GuiHandler() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContainers.STILL_MASTER.get(), StillMasterScreen::new);
    }
}
