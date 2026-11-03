package io.vinum.client;

import io.vinum.bar.BarInteractions;
import io.vinum.network.BarUseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="vinum",value=Dist.CLIENT)
public final class BarInteractionClient {
    private BarInteractionClient() {}
    @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        var mc=Minecraft.getInstance();
        if(!event.isUseItem()||mc.player==null||mc.level==null||mc.screen!=null||mc.player.isSpectator()
            ||BarInteractions.target(mc.player).isEmpty())return;
        // NeoForge fires both hands even when canceled: exactly one intent per click.
        event.setCanceled(true);event.setSwingHand(false);
        InteractionHand hand=mc.player.getMainHandItem().isEmpty()&&!mc.player.getOffhandItem().isEmpty()
            ?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        if(event.getHand()!=hand)return;
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
        PacketDistributor.sendToServer(new BarUseMessage(hand));
        mc.player.swing(hand);
    }
}
