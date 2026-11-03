package io.vinum.test.client;

import io.vinum.item.*;
import io.vinum.fluid.ModFluids;
import io.vinum.production.ProductionInputs;
import io.vinum.sugar.*;
import java.util.List;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Real menu packets exercise loading, processing, recycling and both outputs. */
final class SugarPlantScenario implements ClientScenario {
    public String name(){return "sugar_plant";}
    private static SugarPlantMenu menu(ClientTestContext c){return (SugarPlantMenu)c.mc.player.containerMenu;}
    private static void button(ClientTestContext c,int id){c.mc.gameMode.handleInventoryButtonClick(menu(c).containerId,id);}
    private static void move(ClientTestContext c,int slot){c.mc.gameMode.handleInventoryMouseClick(menu(c).containerId,slot,0,ClickType.QUICK_MOVE,c.mc.player);}
    public List<Step> steps(ClientTestContext c){
        int[] screenshotTicks={0};
        return new Sequence()
            .await("prepare isolated fixture",c::prepare,c::serverDone,200)
            .await("place plant and supply cane juice carboy and fuel",()->c.server(p->{
                p.serverLevel().setBlockAndUpdate(c.target,SugarContent.PLANT.get().defaultBlockState());
                var juice=new FluidStack(ModFluids.CANE_JUICE.source().get(),1000);
                ProductionInputs.defaultProfile(juice).applyTo(juice);
                c.equip(p,0,((FluidContainerItem)ModItems.JUICE_CARBOY.get()).filled(juice));
                c.equip(p,1,new ItemStack(Items.COAL));
            }),c::serverDone,100)
            .await("wait for fixture synchronization",()->{},()->c.mc.level.getBlockState(c.target).is(SugarContent.PLANT.get())
                &&c.mc.player.getInventory().getItem(1).is(Items.COAL),200)
            .action("open plant with empty hand",()->{c.select(2);c.crouch(false);c.useBlock();})
            .await("actual plant screen opens",()->{},()->c.mc.player.containerMenu instanceof SugarPlantMenu&&c.mc.screen instanceof SugarPlantScreen,200)
            .action("move cursor clear of GUI",()->org.lwjgl.glfw.GLFW.glfwSetCursorPos(c.mc.getWindow().getWindow(),0,0))
            .action("shift click juice carboy into input",()->move(c,34))
            .await("input tank and returned carboy synchronize",()->{},()->menu(c).inputAmount()==1000
                &&menu(c).getSlot(1).getItem().is(ModItems.JUICE_CARBOY.get()),200)
            .action("shift click coal into fuel",()->move(c,35))
            .await("fuel slot synchronizes",()->{},()->menu(c).getSlot(4).getItem().is(Items.COAL),100)
            .action("start concentration through menu packet",()->button(c,100))
            .await("processing progress synchronizes",()->{},()->menu(c).progress()>5&&menu(c).required()==200,100)
            .action("capture processing GUI",()->Screenshot.grab(c.mc.gameDirectory,"sugar-plant-processing.png",c.mc.getMainRenderTarget(),message->{}))
            .await("syrup output synchronizes",()->{},()->menu(c).outputAmount()==250&&menu(c).required()==0,300)
            .action("recycle syrup through GUI",()->button(c,101))
            .await("recycled input synchronizes",()->{},()->menu(c).inputAmount()==250&&menu(c).outputAmount()==0,100)
            .action("select separation through GUI",()->button(c,1))
            .await("mode synchronizes",()->{},()->menu(c).mode()==1,100)
            .action("start built-in separation",()->button(c,100))
            .await("sugar and molasses synchronize",()->{},()->menu(c).outputAmount()==250
                &&menu(c).getSlot(6).getItem().is(Items.SUGAR),400)
            .await("allow completed GUI to render",()->{},()->++screenshotTicks[0]>20,100)
            .action("capture completed separation GUI",()->Screenshot.grab(c.mc.gameDirectory,"sugar-plant-separated.png",c.mc.getMainRenderTarget(),message->{}))
            .await("verify authoritative outputs and conservation",()->c.server(p->{
                var plant=(SugarPlantBlockEntity)p.serverLevel().getBlockEntity(c.target);
                if(plant.processing()||!plant.input().isEmpty()||plant.output().getAmount()!=250
                    ||plant.inventory.getStackInSlot(6).getCount()!=1)
                    throw new AssertionError("Server plant did not finish the two-stage sugar route");
            }),c::serverDone,100)
            .action("close actual menu",()->c.mc.player.closeContainer()).build();
    }
}
