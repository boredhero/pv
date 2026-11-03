package io.vinum.test.client;

import io.vinum.blender.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.item.*;
import io.vinum.production.BatchProfile;
import io.vinum.sugar.SugarContent;
import java.util.List;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;

/** Actual container clicks, ratio control and server menu packets for all three cold blender routes. */
final class BlenderScenario implements ClientScenario {
    private int settleTicks;
    private static BlenderMenu menu(ClientTestContext c){return (BlenderMenu)c.mc.player.containerMenu;}
    private static BlenderBlockEntity plant(ClientTestContext c){return (BlenderBlockEntity)c.mc.level.getBlockEntity(c.target);}
    private static void button(ClientTestContext c,int id){c.mc.gameMode.handleInventoryButtonClick(menu(c).containerId,id);}
    private static void click(ClientTestContext c,int slot){c.mc.gameMode.handleInventoryMouseClick(menu(c).containerId,slot,0,ClickType.PICKUP,c.mc.player);}
    private static void transfer(ClientTestContext c,int from,int to){click(c,from);click(c,to);}
    private static FluidContainerItem carboy(){return (FluidContainerItem)ModItems.JUICE_CARBOY.get();}
    public String name(){return "blender";}

    public List<Step> steps(ClientTestContext c){
        var sequence=new Sequence()
            .await("prepare isolated blender fixture",c::prepare,c::serverDone,200)
            .await("place blender and provide two wine batches",()->c.server(p->{
                p.serverLevel().setBlockAndUpdate(c.target,BlenderContent.BLENDER.get().defaultBlockState());
                c.equip(p,0,carboy().filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),1000)));
                c.equip(p,1,carboy().filled(WineCatalog.sample(WineCatalog.varieties().getLast(),3000)));
                c.equip(p,2,new ItemStack(carboy()));
            }),c::serverDone,100)
            .await("wait for blender and carboys",()->{},()->c.mc.level.getBlockEntity(c.target) instanceof BlenderBlockEntity
                &&carboy().contents(c.mc.player.getInventory().getItem(1)).getAmount()==3000,200)
            .action("open actual blender GUI",()->{c.select(3);c.crouch(false);c.useBlock();})
            .await("blender screen opens",()->{},()->c.mc.player.containerMenu instanceof BlenderMenu&&c.mc.screen instanceof BlenderScreen,100)
            .action("load A using actual inventory pickup clicks",()->transfer(c,35,BlenderBlockEntity.A_CONTAINER))
            .await("A tank and empty return synchronize",()->{},()->plant(c).a().getAmount()==1000
                &&menu(c).getSlot(BlenderBlockEntity.A_RETURN).getItem().is(carboy()),200)
            .action("load B using actual inventory pickup clicks",()->transfer(c,36,BlenderBlockEntity.B_CONTAINER))
            .await("B tank and empty return synchronize",()->{},()->plant(c).b().getAmount()==3000
                &&menu(c).getSlot(BlenderBlockEntity.B_RETURN).getItem().is(carboy()),200)
            .action("select twenty-five percent A with the actual slider",()->slide(c,.25))
            .await("ratio setting synchronizes",()->{},()->menu(c).ratio()==25,100)
            .action("start one blend through GUI packet",()->button(c,100))
            .await("first exact-ratio batch finishes",()->{},()->plant(c).output().getAmount()==1000
                &&plant(c).a().getAmount()==750&&plant(c).b().getAmount()==2250,200)
            .action("load empty output carboy using inventory clicks",()->transfer(c,37,BlenderBlockEntity.OUTPUT_CONTAINER))
            .await("filled output carboy synchronizes",()->{},()->carboy().contents(menu(c).getSlot(BlenderBlockEntity.FILLED_CONTAINER).getItem()).getAmount()==1000
                &&plant(c).output().isEmpty(),200)
            .action("enable repeated blending",()->button(c,103))
            .await("automatic mode synchronizes",()->{},()->menu(c).automatic(),100)
            .await("repeated batches consume remaining inputs",()->{},()->plant(c).a().isEmpty()&&plant(c).b().isEmpty()
                &&plant(c).output().getAmount()==3000&&!plant(c).processing(),300)
            .await("verify authoritative mixture and output containers",()->c.server(p->{
                var blender=(BlenderBlockEntity)p.serverLevel().getBlockEntity(c.target);
                assertWineBlend(blender.output(),3000);
                assertWineBlend(carboy().contents(blender.inventory.getStackInSlot(BlenderBlockEntity.FILLED_CONTAINER)),1000);
                if(!blender.a().isEmpty()||!blender.b().isEmpty()||blender.processing())throw new AssertionError("Blend inputs not completely consumed");
            }),c::serverDone,100)
            .await("settle blended GUI",()->settleTicks=0,()->settle(c),100)
            .action("capture exact-ratio blender GUI",()->capture(c,"blender-wine.png"))
            .action("disable automatic blending",()->button(c,103))
            .await("manual mode synchronizes",()->{},()->!menu(c).automatic(),100)
            .action("close blend menu",()->c.mc.player.closeContainer());
        preparation(sequence,c,BlenderBlockEntity.HONEY,new ItemStack(Items.HONEY_BOTTLE),"mead_must",1250,"blender-honey.png");
        preparation(sequence,c,BlenderBlockEntity.WASH,new ItemStack(SugarContent.PANELA.get()),"cane_wash",1000,"blender-wash.png");
        proof(sequence,c);
        return sequence.build();
    }

    private void preparation(Sequence sequence,ClientTestContext c,int mode,ItemStack ingredient,String fluidId,int amount,String screenshot){
        sequence.await("place fresh preparation blender for "+fluidId,()->c.server(p->{
                c.target=c.target.east();
                p.serverLevel().setBlockAndUpdate(c.target,BlenderContent.BLENDER.get().defaultBlockState());
                c.equip(p,0,new ItemStack(Items.WATER_BUCKET));c.equip(p,1,ingredient.copy());c.equip(p,2,new ItemStack(carboy()));
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY(),c.target.getZ()+2.5,180,30);p.setYHeadRot(180);
            }),c::serverDone,100)
            .await("wait for preparation fixture "+fluidId,()->{},()->c.mc.level.getBlockEntity(c.target) instanceof BlenderBlockEntity
                &&c.mc.player.getInventory().getItem(0).is(Items.WATER_BUCKET),200)
            .action("open preparation GUI "+fluidId,()->{c.select(3);c.useBlock();})
            .await("preparation GUI opens "+fluidId,()->{},()->c.mc.player.containerMenu instanceof BlenderMenu,100)
            .action("select preparation recipe "+fluidId,()->button(c,mode))
            .await("preparation mode synchronizes "+fluidId,()->{},()->menu(c).mode()==mode,100)
            .action("load water and ingredient through GUI "+fluidId,()->{
                transfer(c,35,BlenderBlockEntity.A_CONTAINER);transfer(c,36,BlenderBlockEntity.INGREDIENT);
            })
            .await("water and ingredient synchronize "+fluidId,()->{},()->plant(c).a().getAmount()==1000
                &&menu(c).getSlot(BlenderBlockEntity.INGREDIENT).getItem().is(ingredient.getItem()),200)
            .action("start preparation "+fluidId,()->button(c,100))
            .await("prepared liquid synchronizes "+fluidId,()->{},()->plant(c).output().getAmount()==amount&&!plant(c).processing(),200)
            .await("verify prepared liquid and containers "+fluidId,()->c.server(p->{
                var blender=(BlenderBlockEntity)p.serverLevel().getBlockEntity(c.target);
                var result=blender.output();
                if(!BuiltInRegistries.FLUID.getKey(result.getFluid()).getPath().equals(fluidId)
                    ||result.getAmount()!=amount||Math.abs(BatchProfile.of(result).sugarGramsPerLitre()-200)>1e-8
                    ||!blender.inventory.getStackInSlot(BlenderBlockEntity.INGREDIENT).isEmpty()
                    ||!blender.inventory.getStackInSlot(BlenderBlockEntity.A_RETURN).is(Items.BUCKET))
                    throw new AssertionError("Incorrect preparation output or container conservation: "+fluidId);
                if(mode==BlenderBlockEntity.HONEY&&!blender.inventory.getStackInSlot(BlenderBlockEntity.INGREDIENT_RETURN).is(Items.GLASS_BOTTLE))
                    throw new AssertionError("Honey preparation lost the returned glass bottle");
            }),c::serverDone,100)
            .await("settle preparation GUI "+fluidId,()->settleTicks=0,()->settle(c),100)
            .action("capture preparation GUI "+fluidId,()->capture(c,screenshot))
            .action("close preparation GUI "+fluidId,()->c.mc.player.closeContainer());
    }

    private void proof(Sequence sequence,ClientTestContext c){
        sequence.await("place proofing fixture and supply strong rum and water",()->c.server(p->{
                c.target=c.target.offset(-3,0,0);
                p.serverLevel().setBlockAndUpdate(c.target,BlenderContent.BLENDER.get().defaultBlockState());
                var rum=ModItems.spirit(io.vinum.fluid.ModFluids.RUM.source().get(),1000);
                BatchProfile.of(rum).withAlcoholFraction(.6).applyTo(rum);
                c.equip(p,0,carboy().filled(rum));c.equip(p,1,new ItemStack(Items.WATER_BUCKET));
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY(),c.target.getZ()+2.5,180,30);p.setYHeadRot(180);
            }),c::serverDone,100)
            .await("wait for proofing fixture",()->{},()->c.mc.level.getBlockEntity(c.target) instanceof BlenderBlockEntity
                &&carboy().contents(c.mc.player.getInventory().getItem(0)).getAmount()==1000,200)
            .action("open proofing GUI",()->{c.select(3);c.useBlock();})
            .await("proofing GUI opens",()->{},()->c.mc.player.containerMenu instanceof BlenderMenu,100)
            .action("select target ABV mode",()->button(c,3))
            .await("target ABV mode synchronizes",()->{},()->menu(c).mode()==3,100)
            .action("set thirty-five percent target with actual slider",()->slide(c,349/949D))
            .await("changed ABV target synchronizes",()->{},()->menu(c).targetAbv()==350,100)
            .action("set forty percent target with actual slider",()->slide(c,399/949D))
            .await("ABV target synchronizes",()->{},()->menu(c).targetAbv()==400,100)
            .action("load strong rum and water through GUI",()->{
                transfer(c,35,BlenderBlockEntity.A_CONTAINER);transfer(c,36,BlenderBlockEntity.B_CONTAINER);
            })
            .await("proofing inputs synchronize",()->{},()->plant(c).a().getAmount()==1000&&plant(c).b().getAmount()==1000,200)
            .action("start target ABV proofing",()->button(c,100))
            .await("proofed output synchronizes",()->{},()->plant(c).output().getAmount()==999&&!plant(c).processing(),200)
            .await("verify authoritative alcohol and water conservation",()->c.server(p->{
                var blender=(BlenderBlockEntity)p.serverLevel().getBlockEntity(c.target);
                if(blender.output().getAmount()!=999||Math.abs(BatchProfile.of(blender.output()).alcoholFraction()-.4)>1e-9
                    ||blender.a().getAmount()!=334||blender.b().getAmount()!=667)
                    throw new AssertionError("Proofing did not consume666mB spirit +333mB water for999mB at40percent");
            }),c::serverDone,100)
            .await("settle proofing GUI",()->settleTicks=0,()->settle(c),100)
            .action("capture proofing GUI",()->capture(c,"blender-proof.png"))
            .action("close proofing GUI",()->c.mc.player.closeContainer());
    }

    private static void slide(ClientTestContext c,double value){
        var slider=c.mc.screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.AbstractSliderButton)
            .map(child->(net.minecraft.client.gui.components.AbstractSliderButton)child).findFirst().orElseThrow();
        double x=slider.getX()+4+(slider.getWidth()-8)*value,y=slider.getY()+slider.getHeight()/2D;
        if(!c.mc.screen.mouseClicked(x,y,0))throw new AssertionError("Blender slider rejected click");
        c.mc.screen.mouseReleased(x,y,0);
    }

    private boolean settle(ClientTestContext c){c.mc.getToasts().clear();org.lwjgl.glfw.GLFW.glfwSetCursorPos(c.mc.getWindow().getWindow(),0,0);return ++settleTicks>20;}
    private static void capture(ClientTestContext c,String name){Screenshot.grab(c.mc.gameDirectory,name,c.mc.getMainRenderTarget(),message->{});}
    private static void assertWineBlend(FluidStack fluid,int amount){
        var first=WineCatalog.varieties().getFirst();var last=WineCatalog.varieties().getLast();
        var profile=BatchProfile.of(fluid);
        if(fluid.getAmount()!=amount||Math.abs(profile.ingredientProportions().getOrDefault(first.ingredient(),0D)-.25)>1e-9
            ||Math.abs(profile.ingredientProportions().getOrDefault(last.ingredient(),0D)-.75)>1e-9
            ||Math.abs(profile.alcoholFraction()-(first.defaultAlcohol()*.25+last.defaultAlcohol()*.75))>1e-9)
            throw new AssertionError("Blender failed exact 25/75 proportions or alcohol conservation");
    }
}
