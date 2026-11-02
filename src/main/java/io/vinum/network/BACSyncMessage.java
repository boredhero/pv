package io.vinum.network;

import io.vinum.common.Defines;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Only the visible level is needed by the client; the decay timer stays server-side. */
public record BACSyncMessage(int level) implements CustomPacketPayload {
    public static final Type<BACSyncMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Defines.MODID, "bac_sync"));
    public static final StreamCodec<FriendlyByteBuf, BACSyncMessage> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BACSyncMessage::level, BACSyncMessage::new);
    @Override
    public Type<BACSyncMessage> type() { return TYPE; }
}
