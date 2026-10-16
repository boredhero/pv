package io.vinum.client;

import io.vinum.capability.BACCapability;
import io.vinum.common.Defines;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Client-only registration keeps HUD classes off dedicated servers. */
@EventBusSubscriber(modid = Defines.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BACHud {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Defines.MODID, "textures/gui/bac_hud_bar.png");
    private BACHud() {}
    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Defines.MODID, "bac"), (graphics, deltaTracker) -> {
            Minecraft minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (minecraft.options.hideGui || player == null || player.isCreative() || player.isSpectator()) return;
            int bac = player.getData(BACCapability.BAC_STATE).getBACLevel();
            if (bac == 0) return;
            int x = graphics.guiWidth() / 2 + 10;
            int y = graphics.guiHeight() - (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply() ? 59 : 49);
            for (int i = 0; i < 9; i++) {
                graphics.blit(TEXTURE, x + i * 9, y, i < bac ? 9 : 0, 0, 9, 9);
            }
        });
    }
}
