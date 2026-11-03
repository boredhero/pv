package io.vinum.client;

import io.vinum.bar.*;
import io.vinum.beverage.DrinkwareAppearance;
import io.vinum.network.BarDrinkMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="vinum",value=Dist.CLIENT)
public final class BarDrinkClient {
    private static BlockPos target;
    private static int slot=-1,ticks;
    private BarDrinkClient() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        var picked=mc.player==null||mc.level==null||mc.screen!=null||!mc.options.keyUse.isDown()
            ||!mc.player.getMainHandItem().isEmpty()||mc.player.isShiftKeyDown()?null:BarInteractions.target(mc.player).orElse(null);
        if(mc.player!=null && mc.level!=null && mc.screen==null && mc.options.keyUse.isDown()
            && mc.player.getMainHandItem().isEmpty() && !mc.player.isShiftKeyDown()
            && picked!=null && mc.level.getBlockEntity(picked.hit().getBlockPos()) instanceof BarBlockEntity bar) {
            var hit=picked.hit();
            int next=picked.slot();
            var drink=bar.bottle(next);
            if(DrinkwareAppearance.canDrink(drink)) {
                if(!hit.getBlockPos().equals(target)||slot!=next){ticks=0;target=hit.getBlockPos().immutable();slot=next;}
                ticks++;
                PacketDistributor.sendToServer(new BarDrinkMessage(target,slot,true));
                mc.player.displayClientMessage(Component.translatable("message.vinum.bar_drinking",Math.min(100,ticks*100/drink.getUseDuration(mc.player))),true);
                return;
            }
        }
        if(target!=null && mc.player!=null)PacketDistributor.sendToServer(new BarDrinkMessage(target,slot,false));
        target=null;slot=-1;ticks=0;
    }
}
