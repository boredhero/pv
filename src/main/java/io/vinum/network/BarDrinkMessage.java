package io.vinum.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The server validates aim, distance, contents and elapsed hold time for every heartbeat. */
public record BarDrinkMessage(BlockPos pos,int slot,boolean active) implements CustomPacketPayload {
    public static final Type<BarDrinkMessage> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("vinum","bar_drink"));
    public static final StreamCodec<FriendlyByteBuf,BarDrinkMessage> STREAM_CODEC=StreamCodec.composite(
        BlockPos.STREAM_CODEC,BarDrinkMessage::pos,ByteBufCodecs.VAR_INT,BarDrinkMessage::slot,
        ByteBufCodecs.BOOL,BarDrinkMessage::active,BarDrinkMessage::new);
    @Override public Type<BarDrinkMessage> type(){return TYPE;}
}
