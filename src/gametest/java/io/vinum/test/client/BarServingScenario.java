package io.vinum.test.client;

import io.vinum.bar.*;
import io.vinum.item.*;
import io.vinum.fluid.ModFluids;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.InputEvent;

/** Exercises the real client input subscriber, payload, server aim validation and inventory synchronization. */
final class BarServingScenario implements ClientScenario {
    public String name(){return "bar_serving";}
    public List<Step> steps(ClientTestContext c) {
        var bottle=(FluidContainerItem)ModItems.RUM_BOTTLE.get();
        var glass=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        return new Sequence()
            .await("prepare isolated fixture and snapshot player",c::prepare,c::serverDone,200)
            .await("place rum bottle and position player at bottle height",()->c.server(p->{
                p.serverLevel().setBlockAndUpdate(c.target,BarContent.BAR.get().defaultBlockState());
                ((BarBlockEntity)p.serverLevel().getBlockEntity(c.target)).insert(2,
                    bottle.filled(ModItems.spirit(ModFluids.RUM.source().get(),750)));
                c.equip(p,0,new ItemStack(glass));
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY()+1.3-p.getEyeHeight(),c.target.getZ()+2.5,180,0);
                p.setYHeadRot(180);
            }),c::serverDone,100)
            .await("wait for bottle and hand synchronization",()->{},()->
                c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar&&!bar.bottle(2).isEmpty()
                    &&c.mc.player.getInventory().getItem(0).is(glass),200)
            .action("right click bottle through actual two-hand input events",()->{
                c.select(0);c.crouch(false);c.mc.player.setXRot(0);c.mc.player.setYRot(180);c.mc.player.setYHeadRot(180);
                if(BarInteractions.target(c.mc.player).isEmpty())throw new AssertionError("Client did not target bottle above counter");
                for(var hand:InteractionHand.values()) {
                    var event=new InputEvent.InteractionKeyMappingTriggered(1,c.mc.options.keyUse,hand);
                    NeoForge.EVENT_BUS.post(event);
                    if(!event.isCanceled())throw new AssertionError("Bottle input did not intercept vanilla miss");
                }
            })
            .await("assert synced shot and bottle remainder",()->{},()->
                glass.contents(c.mc.player.getMainHandItem()).getAmount()==50
                    &&c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar
                    &&bottle.contents(bar.bottle(2)).getAmount()==700,200)
            .await("assert authoritative single serving",()->c.server(p->{
                if(glass.contents(p.getMainHandItem()).getAmount()!=50
                    ||!(p.serverLevel().getBlockEntity(c.target) instanceof BarBlockEntity bar)
                    ||bottle.contents(bar.bottle(2)).getAmount()!=700)
                    throw new AssertionError("Input packet did not transfer exactly50mB on server");
            }),c::serverDone,100).build();
    }
}
