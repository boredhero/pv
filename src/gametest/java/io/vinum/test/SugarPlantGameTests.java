package io.vinum.test;

import io.vinum.production.*;
import io.vinum.sugar.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class SugarPlantGameTests {
    @GameTest(template="barrel_space")
    public static void solidOutputBlocksBeforeInputsOrFuelAreConsumed(GameTestHelper h){
        var p=plant(h);p.inputFluids().fill(fluid("cane_syrup",250),FluidAction.EXECUTE);p.selectMode(1);
        p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.COAL));
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));
        h.assertTrue(!p.start()&&p.status()==p.STATUS_SOLID&&p.input().getAmount()==250
            &&p.inventory.getStackInSlot(p.FUEL).getCount()==1,"Blocked solid output reserves no batch or fuel");
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,ItemStack.EMPTY);h.assertTrue(p.start(),"Clearing output permits separation");
        tick(h,p,1);int fuel=p.burnTime();
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));tick(h,p,30);
        h.assertTrue(p.progress()==1&&p.burnTime()==fuel,"A blocked running batch wastes no fuel and retains progress");
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,ItemStack.EMPTY);tick(h,p,299);
        h.assertTrue(p.output().getAmount()==250&&p.inventory.getStackInSlot(p.SOLID_OUTPUT).is(Items.SUGAR),"Resumed batch produces both outputs once");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void incompatibleFluidOutputAndAutomationCannotOverwriteProduct(GameTestHelper h){
        var p=plant(h);p.inputFluids().fill(fluid("cane_juice",1000),FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.COAL));h.assertTrue(p.start(),"Concentration starts");tick(h,p,200);
        p.inputFluids().fill(fluid("cane_syrup",250),FluidAction.EXECUTE);p.selectMode(1);int fuel=p.burnTime();
        h.assertTrue(!p.start()&&p.status()==p.STATUS_OUTPUT&&p.burnTime()==fuel&&p.input().getAmount()==250,
            "Existing syrup blocks molasses output rather than silently blending products");
        h.assertTrue(p.outputFluids().fill(fluid("cane_juice",1000),FluidAction.EXECUTE)==0,"Output port rejects injected fluids");
        h.assertTrue(p.items(Direction.DOWN).insertItem(0,new ItemStack(Items.COAL),false).getCount()==1,
            "Bottom output automation rejects item insertion");
        h.assertTrue(p.outputFluids().drain(250,FluidAction.SIMULATE).getAmount()==250&&p.output().getAmount()==250,
            "Output drain simulation preserves product");
        p.outputFluids().drain(250,FluidAction.EXECUTE);h.assertTrue(p.start(),"Emptying output allows next stage");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void lavaFuelRemainderAndReservedBatchSurviveItemPersistence(GameTestHelper h){
        var p=plant(h);p.inputFluids().fill(fluid("cane_juice",2000),FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(p.start(),"Lava is furnace fuel");tick(h,p,75);
        h.assertTrue(p.inventory.getStackInSlot(p.FUEL).is(Items.BUCKET),"Lava returns its empty bucket");
        var item=new ItemStack(SugarContent.PLANT_ITEM.get());p.saveToItem(item,h.getLevel().registryAccess());
        var stored=item.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(stored!=null,"Pickup carries block entity data");
        var copy=new SugarPlantBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(stored.copyTag(),h.getLevel().registryAccess());
        h.assertTrue(copy.processing()&&copy.progress()==75&&copy.input().getAmount()==1000&&copy.burnTime()==p.burnTime()
            &&copy.inventory.getStackInSlot(copy.FUEL).is(Items.BUCKET),"Pickup persists reserved batch, remaining input, fuel and container");
        tick(h,copy,125);h.assertTrue(copy.output().getAmount()==250&&!copy.processing(),"Restored batch completes exactly once");
        int fuel=copy.burnTime();tick(h,copy,200);h.assertTrue(copy.output().getAmount()==250&&copy.burnTime()==fuel,"Idle plant neither repeats outputs nor wastes fuel");h.succeed();
    }

    @GameTest(template="barrel_space")
    public static void automaticBatchesWaitForFuelAndOutputThenResume(GameTestHelper h){
        var p=plant(h);p.selectMode(1);p.inputFluids().fill(fluid("cane_syrup",750),FluidAction.EXECUTE);
        p.toggleAutomatic();tick(h,p,20);
        h.assertTrue(p.automatic()&&!p.processing()&&p.input().getAmount()==750,"Auto waits for fuel without reserving input");
        p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.COAL));
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));tick(h,p,20);
        h.assertTrue(p.input().getAmount()==750&&p.inventory.getStackInSlot(p.FUEL).getCount()==1,"Blocked auto start consumes neither input nor fuel");
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,ItemStack.EMPTY);tick(h,p,300);
        h.assertTrue(p.output().getAmount()==250,"First automatic batch completes");
        int remainingFuel=p.burnTime();
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));tick(h,p,50);
        h.assertTrue(p.input().getAmount()==500&&p.burnTime()==remainingFuel,"Output pause between automatic batches wastes nothing");
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,ItemStack.EMPTY);tick(h,p,600);
        h.assertTrue(p.input().isEmpty()&&p.output().getAmount()==750&&p.inventory.getStackInSlot(p.SOLID_OUTPUT).getCount()==2,
            "Clearing the output automatically completes both remaining batches");
        remainingFuel=p.burnTime();tick(h,p,100);
        h.assertTrue(!p.processing()&&p.burnTime()==remainingFuel&&p.output().getAmount()==750,"Empty input waits without duplicate output or fuel waste");
        p.inputFluids().fill(fluid("cane_syrup",250),FluidAction.EXECUTE);tick(h,p,300);
        h.assertTrue(p.output().getAmount()==1000,"Replenished input resumes automatically");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void automaticSettingPersistsAndStopFinishesOnlyReservedBatch(GameTestHelper h){
        var p=plant(h);p.inputFluids().fill(fluid("cane_juice",2500),FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.COAL));p.toggleAutomatic();tick(h,p,75);
        var item=new ItemStack(SugarContent.PLANT_ITEM.get());p.saveToItem(item,h.getLevel().registryAccess());
        var copy=new SugarPlantBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(item.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag(),h.getLevel().registryAccess());
        h.assertTrue(copy.automatic()&&copy.progress()==75,"Pickup and reload preserve repetition and progress");
        copy.toggleAutomatic();tick(h,copy,500);
        h.assertTrue(!copy.automatic()&&!copy.processing()&&copy.output().getAmount()==250&&copy.input().getAmount()==1500,
            "Stopping auto finishes the reserved batch but never starts another");
        copy.toggleAutomatic();tick(h,copy,500);
        h.assertTrue(copy.output().getAmount()==500&&copy.input().getAmount()==500&&!copy.processing(),
            "Automatic processing preserves an insufficient partial batch");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void feHeatingFallsBackToFuelWithoutDoubleCharging(GameTestHelper h){
        var p=plant(h);p.inputFluids().fill(fluid("cane_juice",2000),FluidAction.EXECUTE);
        p.energy.generate(4020);p.inventory.setStackInSlot(p.FUEL,new ItemStack(Items.COAL));p.toggleAutomatic();tick(h,p,200);
        h.assertTrue(p.output().getAmount()==250&&p.energy.getEnergyStored()==20&&p.burnTime()==0
            &&p.inventory.getStackInSlot(p.FUEL).getCount()==1,"FE supplies exactly 20 per tick without igniting available fuel");
        tick(h,p,1);h.assertTrue(p.progress()==1&&p.energy.getEnergyStored()==0,"Next batch spends the final FE tick");
        tick(h,p,1);h.assertTrue(p.progress()==2&&p.burnTime()==1599&&p.inventory.getStackInSlot(p.FUEL).isEmpty(),
            "Fuel takes over when FE runs out, with exactly one burn tick charged");
        p.energy.generate(1234);tick(h,p,198);
        h.assertTrue(p.output().getAmount()==500&&p.energy.getEnergyStored()==1234&&p.burnTime()==1401,
            "Already-lit furnace heat finishes the batch without double-charging FE");
        var item=new ItemStack(SugarContent.PLANT_ITEM.get());p.saveToItem(item,h.getLevel().registryAccess());
        var copy=new SugarPlantBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());
        copy.loadWithComponents(item.get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA).copyTag(),h.getLevel().registryAccess());
        h.assertTrue(copy.energy.getEnergyStored()==1234&&copy.automatic(),"Pickup preserves energy and auto mode");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void fePortsAndBlockedWorkConserveEnergy(GameTestHelper h){
        var p=plant(h);
        for(var side:Direction.values()){
            var port=h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,p.getBlockPos(),side);
            h.assertTrue(port!=null&&port.canReceive()&&!port.canExtract(),"Every face accepts FE and cannot export it");
            h.assertTrue(port.receiveEnergy(1000,true)==80&&port.getEnergyStored()==0,"Simulated intake is bounded and does not mutate");
        }
        p.energy.generate(4000);p.selectMode(1);p.inputFluids().fill(fluid("cane_syrup",250),FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));p.toggleAutomatic();tick(h,p,10);
        h.assertTrue(p.energy.getEnergyStored()==4000&&p.input().getAmount()==250,"Blocked auto start consumes no FE or inputs");
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,ItemStack.EMPTY);tick(h,p,10);
        p.inventory.setStackInSlot(p.SOLID_OUTPUT,new ItemStack(Items.COBBLESTONE,64));tick(h,p,10);
        h.assertTrue(p.energy.getEnergyStored()==3800&&p.progress()==10,"A blocked reserved batch retains its FE and progress");h.succeed();
    }

    private static SugarPlantBlockEntity plant(GameTestHelper h){var pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(pos,SugarContent.PLANT.get().defaultBlockState());return (SugarPlantBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static FluidStack fluid(String id,int amount){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)),amount);return ProductionInputs.defaultProfile(f).applyTo(f);}
    private static void tick(GameTestHelper h,SugarPlantBlockEntity p,int count){for(int i=0;i<count;i++)SugarPlantBlockEntity.tick(h.getLevel(),p.getBlockPos(),p.getBlockState(),p);}
}
