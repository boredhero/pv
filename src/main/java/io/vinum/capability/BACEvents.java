package io.vinum.capability;

import io.vinum.common.Defines;
import io.vinum.item.drinks.IDrink;
import io.vinum.network.NetworkLoader;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Defines.MODID)
public final class BACEvents {
    private BACEvents() {}
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        BAC state = player.getData(BACCapability.BAC_STATE);
        if (state.tick()) NetworkLoader.sync(player);
        // Refresh sustained effects once per second instead of allocating effects every tick.
        if (state.getBACLevel() > 0 && player.tickCount % 20 == 0) {
            IDrink.updatePlayerBAC(player, state.getBACLevel());
        }
    }
    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event); }
    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event); }
    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) NetworkLoader.sync(player);
    }
}
