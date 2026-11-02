package io.vinum.network;

import io.vinum.capability.BACCapability;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkLoader {
    private NetworkLoader() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(BACSyncMessage.TYPE, BACSyncMessage.STREAM_CODEC,
                (message, context) -> context.player().getData(BACCapability.BAC_STATE).setBACLevel(message.level()));
    }
    public static void sync(ServerPlayer player) {
        // Automation players have no client (NeoForge's FakePlayer even has a null Netty channel).
        // Keep their server-side BAC functional without attempting a clientbound HUD update.
        if (player.isFakePlayer() || player.connection == null
                || player.connection.getConnection().channel() == null
                || !player.connection.hasChannel(BACSyncMessage.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new BACSyncMessage(player.getData(BACCapability.BAC_STATE).getBACLevel()));
    }
}
