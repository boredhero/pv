package io.vinum.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Bottle clicks above the block cannot use vanilla's in-block hit-coordinate bounds. */
public record BarUseMessage(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<BarUseMessage> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("vinum","bar_use"));
    public static final StreamCodec<FriendlyByteBuf,BarUseMessage> STREAM_CODEC=StreamCodec.of(
        (buffer,message)->buffer.writeEnum(message.hand()),buffer->new BarUseMessage(buffer.readEnum(InteractionHand.class)));
    @Override public Type<BarUseMessage> type(){return TYPE;}
}
