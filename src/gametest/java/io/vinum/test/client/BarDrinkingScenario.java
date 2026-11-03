package io.vinum.test.client;

import io.vinum.bar.*;
import io.vinum.beverage.*;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Hold the actual use key: only the ordinary client heartbeat subscriber may finish these drinks. */
final class BarDrinkingScenario implements ClientScenario {
    public String name(){return "bar_drinking";}
    public List<Step> steps(ClientTestContext c) {
        var shot=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        var wine=(FluidContainerItem)ModItems.WINE_GLASS_EMPTY.get();
        var drinks=List.of(shot.filled(ModItems.spirit(ModFluids.RUM.source().get(),50)),
            wine.filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),125)),
            new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()));
        var emptyItems=List.of(ModItems.SHOT_GLASS.get(),ModItems.WINE_GLASS_EMPTY.get(),ModItems.MARGARITA_GLASS.get());
        var sequence=new Sequence().await("prepare isolated drinking fixture",c::prepare,c::serverDone,200);
        for(var mode:new GameType[]{GameType.CREATIVE,GameType.SURVIVAL})for(int index=0;index<drinks.size();index++) {
            var drink=drinks.get(index);var empty=emptyItems.get(index);String label=mode.getName()+" "+empty.getDescriptionId();
            sequence
                .action(label+": release previous input",()->{c.mc.options.keyUse.setDown(false);c.crouch(false);})
                .await(label+": set authoritative drink and game mode",()->c.server(p->{
                    p.setGameMode(mode);p.getAbilities().flying=false;p.onUpdateAbilities();
                    p.getInventory().clearContent();p.removeAllEffects();
                    p.serverLevel().setBlockAndUpdate(c.target,BarContent.BAR.get().defaultBlockState());
                    var bar=(BarBlockEntity)p.serverLevel().getBlockEntity(c.target);
                    for(int slot=0;slot<BarBlockEntity.SLOTS;slot++)bar.remove(slot);
                    bar.insert(2,drink);
                    p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY(),c.target.getZ()+2.5,180,0);
                    p.setYHeadRot(180);p.inventoryMenu.broadcastChanges();
                }),c::serverDone,100)
                .await(label+": wait for client drink and inventory",()->{},()->
                    c.mc.player.getMainHandItem().isEmpty()&&c.mc.player.isCreative()==mode.isCreative()
                        &&c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar
                        &&ItemStack.isSameItemSameComponents(drink,bar.bottle(2)),200)
                .action(label+": aim and hold real use key",()->{
                    c.select(0);aim(c,drink);
                    if(BarInteractions.target(c.mc.player).isEmpty())throw new AssertionError("No drink under crosshair");
                    c.mc.options.keyUse.setDown(true);
                })
                .await(label+": drink completes and empty glass synchronizes",()->{},()->
                    c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar&&empty(bar.bottle(2),empty),160)
                .action(label+": release use key",()->c.mc.options.keyUse.setDown(false))
                .await(label+": authoritative drink consumption",()->c.server(p->{
                    if(!(p.serverLevel().getBlockEntity(c.target) instanceof BarBlockEntity bar)||!empty(bar.bottle(2),empty)
                        ||!p.getMainHandItem().isEmpty())throw new AssertionError("Drink did not stay on counter as empty glass");
                    if(drink.is(ModItems.SPICED_APPLE_MARGARITA.get())&&!p.hasEffect(MobEffects.LUCK))
                        throw new AssertionError("Placed cocktail did not apply its ordinary effects");
                }),c::serverDone,100)
                .action(label+": crouch to retrieve empty glass",()->c.crouch(true))
                .await(label+": wait for crouched camera",()->{},()->c.mc.player.isCrouching(),100)
                .action(label+": retrieve through real input events",()->{
                    aim(c,new ItemStack(empty));
                    for(var hand:InteractionHand.values())NeoForge.EVENT_BUS.post(
                        new InputEvent.InteractionKeyMappingTriggered(1,c.mc.options.keyUse,hand));
                })
                .await(label+": glass returned exactly once",()->{},()->
                    c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar&&bar.bottle(2).isEmpty()
                        &&c.mc.player.getInventory().countItem(empty)==1,160);
        }
        return sequence.action("release all test inputs",()->{c.mc.options.keyUse.setDown(false);c.crouch(false);}).build();
    }
    private static boolean empty(ItemStack stack,Item item) {
        return stack.is(item)&&(!(item instanceof FluidContainerItem container)||container.contents(stack).isEmpty());
    }
    private static void aim(ClientTestContext c,ItemStack drink) {
        var geometry=DrinkwareAppearance.find(drink).orElseThrow().geometry();
        double y=c.target.getY()+1.008+geometry.height(geometry.sections())*.5;
        double dy=c.mc.player.getEyeY()-y;
        double dx=c.target.getX()+.5-c.mc.player.getX(),dz=c.target.getZ()+.5-c.mc.player.getZ();
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        float pitch=(float)Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
        c.mc.player.setYRot(yaw);c.mc.player.setYHeadRot(yaw);c.mc.player.setXRot(pitch);
        c.mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw,pitch,c.mc.player.onGround()));
    }
}
