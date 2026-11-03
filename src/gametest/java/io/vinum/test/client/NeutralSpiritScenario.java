package io.vinum.test.client;

import io.vinum.bar.*;
import io.vinum.item.*;
import io.vinum.production.*;
import java.util.List;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/** Real bar input and rendering for both new bottles and their matching shot glasses. */
final class NeutralSpiritScenario implements ClientScenario {
    private int ticks;
    private int oldFov;
    public String name(){return "neutral_spirit";}
    private static FluidContainerItem bottle(String name){return (FluidContainerItem)BuiltInRegistries.ITEM.get(ProductionInputs.id(name+"_bottle"));}
    private static FluidStack fluid(String name,int amount,double abv){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(name)),amount);BatchProfile.of(f).withAlcoholFraction(abv).applyTo(f);return f;}
    public List<Step> steps(ClientTestContext c){
        var sequence=new Sequence().await("prepare isolated neutral spirit fixture",c::prepare,c::serverDone,200)
            .action("preserve camera FOV for render closeups",()->{oldFov=c.mc.options.fov().get();c.onRestore(()->c.mc.options.fov().set(oldFov));})
            .await("assemble still for actual mode control",()->c.server(p->{
                var parts=List.of(io.vinum.block.ModBlocks.STEEL_BRAZIER.get(),io.vinum.block.ModBlocks.STEEL_POT.get(),io.vinum.block.ModBlocks.STEEL_COIL.get(),io.vinum.block.ModBlocks.STEEL_POT.get());
                for(int i=1;i<=4;i++)p.serverLevel().setBlockAndUpdate(io.vinum.multiblock.StillMultiblock.position(c.target,net.minecraft.core.Direction.NORTH,i),parts.get(i-1).defaultBlockState());
                io.vinum.multiblock.StillMultiblock.build(p.serverLevel(),c.target);
            }),c::serverDone,100)
            .await("wait for still sync",()->{},()->c.mc.level.getBlockEntity(c.target) instanceof io.vinum.tileentity.StillMasterTileEntity,200)
            .action("open actual still menu",c::useBlock)
            .await("wait for still menu",()->{},()->c.mc.player.containerMenu instanceof io.vinum.inventory.container.StillMasterContainer,100)
            .action("click rectification mode button",()->{
                var button=c.mc.screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button)
                    .map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow();
                c.mc.screen.mouseClicked(button.getX()+5,button.getY()+5,0);c.mc.screen.mouseReleased(button.getX()+5,button.getY()+5,0);
            })
            .await("verify rectification mode synchronized",()->{},()->((io.vinum.inventory.container.StillMasterContainer)c.mc.player.containerMenu).rectification(),100)
            .await("settle still GUI",()->ticks=0,()->++ticks>20,100)
            .action("capture still mode GUI",()->Screenshot.grab(c.mc.gameDirectory,"neutral-still.png",c.mc.getMainRenderTarget(),message->{}))
            .action("close actual still menu",()->c.mc.player.closeContainer())
            .await("stage assembled still and loose parts for inspection",()->c.server(p->{
                p.serverLevel().setBlockAndUpdate(c.target.offset(-2,0,0),io.vinum.block.ModBlocks.STEEL_POT.get().defaultBlockState());
                p.serverLevel().setBlockAndUpdate(c.target.offset(-2,0,-1),io.vinum.block.ModBlocks.STEEL_BRAZIER.get().defaultBlockState());
                p.serverLevel().setBlockAndUpdate(c.target.offset(-2,0,-2),io.vinum.block.ModBlocks.STEEL_COIL.get().defaultBlockState());
                p.teleportTo(p.serverLevel(),c.target.getX()-3,c.target.getY()+1,c.target.getZ()+4,220,20);p.setYHeadRot(220);
            }),c::serverDone,100)
            .await("settle still world rendering",()->ticks=0,()->++ticks>30,100)
            .action("capture assembled still and loose steel components",()->Screenshot.grab(c.mc.gameDirectory,"neutral-still-world.png",c.mc.getMainRenderTarget(),message->{}))
            .await("remove test still before bar fixture",()->c.server(p->{
                for(int i=4;i>=1;i--)p.serverLevel().setBlockAndUpdate(io.vinum.multiblock.StillMultiblock.position(c.target,net.minecraft.core.Direction.NORTH,i),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }),c::serverDone,100);
        for(String name:List.of("neutral_spirit","vodka")){
            sequence.await("place "+name+" bottle and position player",()->c.server(p->{
                p.serverLevel().setBlockAndUpdate(c.target,BarContent.BAR.get().defaultBlockState().setValue(BarBlock.FACING,net.minecraft.core.Direction.SOUTH));
                var bar=(BarBlockEntity)p.serverLevel().getBlockEntity(c.target);
                for(int i=0;i<5;i++)bar.remove(i);
                var drink=fluid(name,750,name.equals("vodka")?.4:.95);
                bar.insert(2,bottle(name).filled(drink));
                c.equip(p,0,new ItemStack(ModItems.SHOT_GLASS.get()));
                c.equip(p,1,bottle("neutral_spirit").filled(fluid("neutral_spirit",750,.95)));
                c.equip(p,2,bottle("vodka").filled(fluid("vodka",750,.4)));
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY()+1.3-p.getEyeHeight(),c.target.getZ()+2.5,180,0);p.setYHeadRot(180);
            }),c::serverDone,100)
            .await("wait for "+name+" fixture sync",()->{},()->c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar
                &&bar.bottle(2).is(bottle(name))&&c.mc.player.getInventory().getItem(0).is(ModItems.SHOT_GLASS.get()),200)
            .action("pour "+name+" through actual right-click input",()->{
                c.select(0);c.crouch(false);c.mc.player.setXRot(0);c.mc.player.setYRot(180);c.mc.player.setYHeadRot(180);
                if(BarInteractions.target(c.mc.player).isEmpty())throw new AssertionError("Bottle not targeted");
                for(var hand:InteractionHand.values()){
                    var event=new InputEvent.InteractionKeyMappingTriggered(1,c.mc.options.keyUse,hand);NeoForge.EVENT_BUS.post(event);
                    if(!event.isCanceled())throw new AssertionError("Placed bottle input not intercepted");
                }
            })
            .await("verify synced "+name+" shot and remainder",()->{},()->((FluidContainerItem)ModItems.SHOT_GLASS.get()).contents(c.mc.player.getMainHandItem()).getAmount()==50
                &&c.mc.level.getBlockEntity(c.target) instanceof BarBlockEntity bar&&bottle(name).contents(bar.bottle(2)).getAmount()==700,200)
            .await("place matching shot beside bottle for render inspection",()->c.server(p->{
                var glass=(FluidContainerItem)ModItems.SHOT_GLASS.get();var serving=glass.contents(p.getMainHandItem());
                if(!BuiltInRegistries.FLUID.getKey(serving.getFluid()).equals(ProductionInputs.id(name)))throw new AssertionError("Shot identity lost");
                ((BarBlockEntity)p.serverLevel().getBlockEntity(c.target)).insert(0,p.getMainHandItem().copy());
                c.equip(p,0,ItemStack.EMPTY);
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY()+.2,c.target.getZ()+2.1,180,20);p.setYHeadRot(180);
            }),c::serverDone,100)
            .await("settle "+name+" rendering",()->{ticks=0;c.mc.options.fov().set(35);},()->{c.mc.getToasts().clear();return ++ticks>30;},100)
            .action("capture "+name+" bottles and shot",()->Screenshot.grab(c.mc.gameDirectory,"neutral-"+name+".png",c.mc.getMainRenderTarget(),message->{}));
        }
        return sequence.build();
    }
}
