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
    public static void twoTrackedSugarItemsMakeOneLitreOfFermentableWash(GameTestHelper h){
        var p=plant(h);p.selectMode(p.DISSOLVE_MODE);
        p.inputFluids().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE);
        var sugar=SugarProcess.recipes().get(1).solids(fluid("cane_syrup",250));
        p.inventory.setStackInSlot(p.INGREDIENT,sugar.copy());
        h.assertTrue(!p.start()&&p.status()==p.STATUS_INGREDIENT&&p.input().getAmount()==1000
            &&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,"One sugar cannot start or consume water");
        p.inventory.setStackInSlot(p.INGREDIENT,sugar.copyWithCount(3));
        h.assertTrue(p.start(),"Two tracked sugar items prepare wash without fuel");tick(h,p,20);
        var profile=BatchProfile.of(p.output());
        h.assertTrue(p.output().getAmount()==1000&&Math.abs(profile.sugarGramsPerLitre()-200)<1e-8
            &&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,"One litre receives exactly the mass of two 100g sugar items");
        h.assertTrue(profile.ingredientProportions().equals(sugar.get(ProductionDataComponents.BATCH_PROFILE.get()).ingredientProportions())
            &&profile.history().contains(ProductionInputs.id("dissolving")),"Wash preserves cane origin and processing history");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
            .stream().anyMatch(r->r.value().matches(p.output())),"Resulting cane sugar wash is fermentable");h.succeed();
    }
    private static SugarPlantBlockEntity plant(GameTestHelper h){var pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(pos,SugarContent.PLANT.get().defaultBlockState());return (SugarPlantBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static FluidStack fluid(String id,int amount){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)),amount);return ProductionInputs.defaultProfile(f).applyTo(f);}
    private static void tick(GameTestHelper h,SugarPlantBlockEntity p,int count){for(int i=0;i<count;i++)SugarPlantBlockEntity.tick(h.getLevel(),p.getBlockPos(),p.getBlockState(),p);}
}
