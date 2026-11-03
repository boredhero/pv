package io.vinum.test;

import io.vinum.blender.*;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.WineCatalog;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.production.*;
import io.vinum.sugar.SugarProcess;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BlenderGameTests {
    @GameTest(template="barrel_space")
    public static void exactQuarterWineBlendConservesVolumeAlcoholAgeAndProvenance(GameTestHelper h){
        var p=blender(h);p.setRatio(25);
        var a=wine(0,1000,.10,100);var b=wine(1,3000,.18,500);
        h.assertTrue(p.inputA().fill(a,FluidAction.SIMULATE)==1000&&p.a().isEmpty(),"Simulating input cannot mutate stored wine");
        h.assertTrue(!p.setRatio(-1)&&!p.setRatio(101)&&p.ratio()==25,"Malformed ratios are rejected without clamping into a different recipe");
        p.inputA().fill(a,FluidAction.EXECUTE);p.inputB().fill(b,FluidAction.EXECUTE);
        h.assertTrue(p.start(),"Compatible wines start a 25:75 batch");
        h.assertTrue(p.a().getAmount()==750&&p.b().getAmount()==2250&&p.output().isEmpty(),"Exactly 250A and750B reserve atomically");
        h.assertTrue(!p.setRatio(50)&&!p.selectMode(p.HONEY),"An active batch cannot change its recipe or ratio");
        tick(h,p,20);var result=p.output();var profile=BatchProfile.of(result);
        h.assertTrue(result.getAmount()==1000&&Math.abs(profile.alcoholFraction()-.16)<1e-9&&BarrelFluidData.getAge(result)==400,
            "25:75 wine blend preserves volume, weighted alcohol and weighted age");
        h.assertTrue(Math.abs(profile.ingredientProportions().get(WineCatalog.varieties().get(0).ingredient())-.25)<1e-9
            &&Math.abs(profile.ingredientProportions().get(WineCatalog.varieties().get(1).ingredient())-.75)<1e-9,"Varietal proportions follow the selected ratio");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void smallerRemaindersKeepExactRatioAndInsufficientSidesRemainUntouched(GameTestHelper h){
        var p=blender(h);p.setRatio(25);
        p.inputA().fill(wine(0,1,.12,0),FluidAction.EXECUTE);p.inputB().fill(wine(1,2,.12,0),FluidAction.EXECUTE);
        h.assertTrue(!p.start()&&p.a().getAmount()==1&&p.b().getAmount()==2,"Less than one exact1:3 unit cannot start or silently alter the ratio");
        p.inputA().fill(wine(0,16,.12,0),FluidAction.EXECUTE);p.inputB().fill(wine(1,98,.12,0),FluidAction.EXECUTE);
        h.assertTrue(p.start(),"A smaller exact-ratio batch starts");tick(h,p,20);
        h.assertTrue(p.output().getAmount()==68&&p.a().isEmpty()&&p.b().getAmount()==49,
            "17A+51B makes68mB; the excess B is never folded into the batch");
        p.outputFluids().drain(8000,FluidAction.EXECUTE);p.inputB().drain(8000,FluidAction.EXECUTE);
        p.setRatio(33);p.inputA().fill(wine(0,32,.12,0),FluidAction.EXECUTE);p.inputB().fill(wine(1,67,.12,0),FluidAction.EXECUTE);
        h.assertTrue(!p.start(),"33:67 requires integral100mB units");
        p.inputA().fill(wine(0,1,.12,0),FluidAction.EXECUTE);h.assertTrue(p.start(),"Exactly33+67 permits100mB");tick(h,p,20);
        h.assertTrue(p.output().getAmount()==100&&p.a().isEmpty()&&p.b().isEmpty(),"One exact smallest unit consumes both sides precisely");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void dilutionPreservesAlcoholMassSourceAgeAndUnknownSourceMetadata(GameTestHelper h){
        var p=blender(h);p.setRatio(25);
        var spirit=BarrelFluidData.withAge(ModItems.spirit(ModFluids.RUM.source().get(),250),12345);
        spirit.set(DataComponents.CUSTOM_NAME,Component.literal("Private batch"));
        p.inputA().fill(spirit,FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,750),FluidAction.EXECUTE);
        h.assertTrue(p.start(),"Spirit and plain water can be proofed");tick(h,p,20);
        var result=p.output();
        h.assertTrue(result.getAmount()==1000&&Math.abs(BatchProfile.of(result).alcoholFraction()-BatchProfile.of(spirit).alcoholFraction()*.25)<1e-9
            &&BarrelFluidData.getAge(result)==12345&&result.get(DataComponents.CUSTOM_NAME).equals(spirit.get(DataComponents.CUSTOM_NAME)),
            "Proofing preserves alcohol mass, spirit age and unknown source metadata");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void incompatibleBatchAndWaterMetadataAreRejectedWithoutLoss(GameTestHelper h){
        var p=blender(h);p.setRatio(50);
        p.inputA().fill(wine(0,500,.12,0),FluidAction.EXECUTE);
        p.inputB().fill(ModItems.spirit(ModFluids.RUM.source().get(),500),FluidAction.EXECUTE);
        h.assertTrue(!p.start()&&p.status()==p.STATUS_INCOMPATIBLE&&p.a().getAmount()==500&&p.b().getAmount()==500,
            "Unrelated beverage families cannot be silently combined");
        p.inputB().drain(8000,FluidAction.EXECUTE);
        var tagged=wine(1,500,.12,0);tagged.set(DataComponents.CUSTOM_NAME,Component.literal("Unique label"));p.inputB().fill(tagged,FluidAction.EXECUTE);
        h.assertTrue(!p.start()&&p.b().get(DataComponents.CUSTOM_NAME).equals(tagged.get(DataComponents.CUSTOM_NAME)),"Conflicting unknown metadata cannot be discarded");
        p.inputB().drain(8000,FluidAction.EXECUTE);
        var water=new FluidStack(Fluids.WATER,500);water.set(DataComponents.CUSTOM_NAME,Component.literal("Modded water"));p.inputB().fill(water,FluidAction.EXECUTE);
        h.assertTrue(!p.start()&&p.a().getAmount()==500&&p.b().getAmount()==500,"Tagged water cannot lose its metadata through dilution");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void blockedOutputAndIngredientReturnDoNotConsumeInputs(GameTestHelper h){
        var p=blender(h);p.setRatio(100);p.inputA().fill(wine(0,1000,.12,0),FluidAction.EXECUTE);
        h.assertTrue(p.start(),"100percent A pass-through supports one-sided batch");tick(h,p,20);
        p.selectMode(p.HONEY);p.inputA().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.INGREDIENT,new ItemStack(Items.HONEY_BOTTLE));
        h.assertTrue(!p.start()&&p.status()==p.STATUS_OUTPUT&&p.a().getAmount()==1000&&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,
            "Existing wine output prevents honey preparation without loss");
        p.outputFluids().drain(8000,FluidAction.EXECUTE);p.inventory.setStackInSlot(p.INGREDIENT_RETURN,new ItemStack(Items.COBBLESTONE));
        h.assertTrue(!p.start()&&p.status()==p.STATUS_RETURN&&p.a().getAmount()==1000&&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,
            "Blocked honey bottle return reserves no ingredient or water");
        p.inventory.setStackInSlot(p.INGREDIENT_RETURN,ItemStack.EMPTY);h.assertTrue(p.start(),"Clearing return allows preparation");tick(h,p,20);
        h.assertTrue(p.output().getAmount()==1250&&p.inventory.getStackInSlot(p.INGREDIENT_RETURN).is(Items.GLASS_BOTTLE),"Prepared must and glass commit once");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void containersRespectReturnSlotsAndBottleTheExactPreparedBatch(GameTestHelper h){
        var p=blender(h);var carboy=(FluidContainerItem)ModItems.JUICE_CARBOY.get();var source=wine(0,1000,.12,100);
        p.inventory.setStackInSlot(p.A_CONTAINER,carboy.filled(source));p.inventory.setStackInSlot(p.A_RETURN,new ItemStack(Items.COBBLESTONE));tick(h,p,1);
        h.assertTrue(p.a().isEmpty()&&carboy.contents(p.inventory.getStackInSlot(p.A_CONTAINER)).getAmount()==1000,"Blocked container return leaves tank and filled input untouched");
        p.inventory.setStackInSlot(p.A_RETURN,ItemStack.EMPTY);tick(h,p,1);
        h.assertTrue(p.a().getAmount()==1000&&p.inventory.getStackInSlot(p.A_CONTAINER).isEmpty()
            &&carboy.contents(p.inventory.getStackInSlot(p.A_RETURN)).isEmpty(),"Successful input transfer returns its empty carboy");
        p.setRatio(100);h.assertTrue(p.start(),"One-sided output batch starts");tick(h,p,20);
        p.inventory.setStackInSlot(p.OUTPUT_CONTAINER,new ItemStack(carboy));tick(h,p,1);
        var filled=carboy.contents(p.inventory.getStackInSlot(p.FILLED_CONTAINER));
        h.assertTrue(p.output().isEmpty()&&filled.getAmount()==1000&&FluidStack.isSameFluidSameComponents(filled,source),"Output container retains all batch components and volume");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void automaticRatioAndReservedBatchPersistAcrossPickup(GameTestHelper h){
        var p=blender(h);p.setRatio(25);p.setTargetAbv(375);p.inputA().fill(wine(0,500,.12,100),FluidAction.EXECUTE);p.inputB().fill(wine(1,1500,.16,300),FluidAction.EXECUTE);
        p.toggleAutomatic();tick(h,p,10);
        var item=new ItemStack(BlenderContent.ITEM.get());p.saveToItem(item,h.getLevel().registryAccess());
        var copy=new BlenderBlockEntity(p.getBlockPos(),p.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(item.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),h.getLevel().registryAccess());
        h.assertTrue(copy.automatic()&&copy.ratio()==25&&copy.targetAbv()==375&&copy.processing()&&copy.progress()==9,"Pickup retains auto setting, ratio and reserved batch progress");
        copy.toggleAutomatic();tick(h,copy,40);
        h.assertTrue(copy.output().getAmount()==1000&&copy.a().getAmount()==250&&copy.b().getAmount()==750,"Turning auto off finishes only the existing reservation");
        copy.toggleAutomatic();tick(h,copy,42);
        h.assertTrue(copy.output().getAmount()==2000&&copy.a().isEmpty()&&copy.b().isEmpty()&&!copy.processing(),"Armed processing repeats then safely waits for input");
        copy.inputA().fill(wine(0,250,.12,100),FluidAction.EXECUTE);copy.inputB().fill(wine(1,750,.16,300),FluidAction.EXECUTE);tick(h,copy,21);
        h.assertTrue(copy.output().getAmount()==3000,"Replenishing both inputs resumes automatic blending");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void coldHoneyAutoAcceptsFlowingWaterAndPreservesUnusedB(GameTestHelper h){
        var p=blender(h);p.selectMode(p.HONEY);p.inputA().fill(new FluidStack(Fluids.FLOWING_WATER,2000),FluidAction.EXECUTE);
        var untouched=wine(0,300,.12,400);p.inputB().fill(untouched,FluidAction.EXECUTE);
        p.inventory.setStackInSlot(p.INGREDIENT,new ItemStack(Items.HONEY_BOTTLE,2));p.toggleAutomatic();tick(h,p,42);
        h.assertTrue(p.output().getAmount()==2500&&p.inventory.getStackInSlot(p.INGREDIENT_RETURN).getCount()==2
            &&p.b().getAmount()==300&&FluidStack.isSameFluidSameComponents(p.b(),untouched),"Both cold honey batches return glass and leave B untouched, including flowing plain water");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void twoTrackedSugarItemsMakeOneLitreOfFermentableWash(GameTestHelper h){
        var p=blender(h);p.selectMode(p.WASH);p.inputA().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        var sugar=SugarProcess.recipes().get(1).solids(fluid("cane_syrup",250));p.inventory.setStackInSlot(p.INGREDIENT,sugar.copy());
        h.assertTrue(!p.start()&&p.a().getAmount()==1000&&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,"One sugar cannot consume water or start a wash");
        p.inventory.setStackInSlot(p.INGREDIENT,sugar.copyWithCount(3));h.assertTrue(p.start(),"Two tracked sugar items prepare wash without energy");tick(h,p,20);
        var profile=BatchProfile.of(p.output());h.assertTrue(p.output().getAmount()==1000&&Math.abs(profile.sugarGramsPerLitre()-200)<1e-9
            &&p.inventory.getStackInSlot(p.INGREDIENT).getCount()==1,"Exactly two100g sugars enter the litre ofwash");
        h.assertTrue(profile.ingredientProportions().equals(sugar.get(ProductionDataComponents.BATCH_PROFILE.get()).ingredientProportions())
            &&profile.history().contains(ProductionInputs.id("dissolving")),"Wash retains provenance and process history");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream().anyMatch(r->r.value().matches(p.output())),"Prepared cane sugar wash is fermentable");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void tequilaDilutionAndSameVariantBlendingRespectExplicitStrength(GameTestHelper h){
        var p=blender(h);p.setRatio(25);
        for(var type:java.util.List.of(ModFluids.TEQUILA.source().get(),ModFluids.GOLDEN_TEQUILA.source().get())){
            var unprofiled=new FluidStack(type,250);
            p.inputA().fill(unprofiled,FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,750),FluidAction.EXECUTE);
            h.assertTrue(p.start(),"Unprofiled tequila has a valid default dilution profile");tick(h,p,20);
            h.assertTrue(Math.abs(BatchProfile.of(p.output()).alcoholFraction()-.10)<1e-9&&p.output().getAmount()==1000,
                "250mB default40percent tequila retains100mB alcohol after dilution");
            p.outputFluids().drain(8000,FluidAction.EXECUTE);
            var zero=ModItems.spirit(type,250);BatchProfile.of(zero).withAlcoholFraction(0).applyTo(zero);
            p.inputA().fill(zero,FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,750),FluidAction.EXECUTE);
            h.assertTrue(p.start(),"Explicit zero strength is a valid batch");tick(h,p,20);
            h.assertTrue(BatchProfile.of(p.output()).alcoholFraction()==0,"Explicit zero ABV must not be replaced by default tequila strength");p.outputFluids().drain(8000,FluidAction.EXECUTE);
        }
        p.setRatio(50);
        var a=BarrelFluidData.withAge(ModItems.spirit(ModFluids.TEQUILA.source().get(),500),200);
        var b=BarrelFluidData.withAge(ModItems.spirit(ModFluids.TEQUILA.source().get(),500),600);
        BatchProfile.of(a).withAlcoholFraction(.2).applyTo(a);BatchProfile.of(b).withAlcoholFraction(.6).applyTo(b);
        a.set(DataComponents.CUSTOM_NAME,Component.literal("Same lot"));b.set(DataComponents.CUSTOM_NAME,Component.literal("Same lot"));
        p.inputA().fill(a,FluidAction.EXECUTE);p.inputB().fill(b,FluidAction.EXECUTE);
        h.assertTrue(p.start(),"Same tequila variant at different strengths can blend");tick(h,p,20);
        h.assertTrue(p.output().getAmount()==1000&&Math.abs(BatchProfile.of(p.output()).alcoholFraction()-.4)<1e-9
            &&BarrelFluidData.getAge(p.output())==400&&p.output().get(DataComponents.CUSTOM_NAME).equals(a.get(DataComponents.CUSTOM_NAME)),
            "Mixed-strength tequila preserves weighted alcohol/age and equal unknown metadata");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void fullOutputPausesAutomaticWorkUntilSpaceIsAvailable(GameTestHelper h){
        var p=blender(h);p.setRatio(100);p.inputA().fill(wine(0,8000,.12,100),FluidAction.EXECUTE);p.toggleAutomatic();tick(h,p,168);
        h.assertTrue(p.output().getAmount()==8000&&p.a().isEmpty(),"Eight batches fill the reservoir without overflowing");
        p.inputA().fill(wine(0,1000,.12,100),FluidAction.EXECUTE);tick(h,p,40);
        h.assertTrue(p.a().getAmount()==1000&&!p.processing()&&p.output().getAmount()==8000,"Full output pauses without reserving or losing input");
        h.assertTrue(p.outputFluids().drain(1000,FluidAction.SIMULATE).getAmount()==1000&&p.output().getAmount()==8000,"Simulating extraction leaves full output intact");
        p.outputFluids().drain(1000,FluidAction.EXECUTE);tick(h,p,21);
        h.assertTrue(p.output().getAmount()==8000&&p.a().isEmpty(),"Making enough space resumes the armed machine");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void targetStrengthSelectsExactIntegerVolumesAndPreservesAlcohol(GameTestHelper h){
        var p=blender(h);p.selectMode(p.TARGET_ABV);p.setTargetAbv(400);
        var spirit=BarrelFluidData.withAge(ModItems.spirit(ModFluids.RUM.source().get(),1000),12345);
        BatchProfile.of(spirit).withAlcoholFraction(.6).applyTo(spirit);
        p.inputA().fill(spirit,FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        h.assertTrue(p.start(),"60percent spirit can be proofed to40percent");
        h.assertTrue(p.a().getAmount()==334&&p.b().getAmount()==667&&!p.setTargetAbv(300),"Reservation uses exact666A+333B and freezes target during work");
        tick(h,p,20);var result=p.output();
        h.assertTrue(result.getAmount()==999&&Math.abs(BatchProfile.of(result).alcoholFraction()-.4)<1e-9
            &&Math.abs(result.getAmount()*BatchProfile.of(result).alcoholFraction()-666*.6)<1e-9
            &&BarrelFluidData.getAge(result)==12345,"Actual999mB product conserves alcohol and sourceage rather than inventing1000mB");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void impossibleTargetStrengthDoesNotConsumeEitherInput(GameTestHelper h){
        var p=blender(h);p.selectMode(p.TARGET_ABV);
        var spirit=ModItems.spirit(ModFluids.RUM.source().get(),1000);BatchProfile.of(spirit).withAlcoholFraction(.6).applyTo(spirit);
        p.inputA().fill(spirit,FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        for(int target:new int[]{600,700}){
            p.setTargetAbv(target);h.assertTrue(!p.start()&&p.a().getAmount()==1000&&p.b().getAmount()==1000,"Water cannot increase or preserve the source target strength through dilution");
        }
        h.assertTrue(!p.setTargetAbv(0)&&!p.setTargetAbv(951),"Out-of-range requested strength is rejected");
        p.inputA().drain(8000,FluidAction.EXECUTE);p.inputB().drain(8000,FluidAction.EXECUTE);
        p.inputA().fill(spirit.copyWithAmount(1),FluidAction.EXECUTE);p.inputB().fill(new FluidStack(Fluids.WATER,1),FluidAction.EXECUTE);p.setTargetAbv(400);
        h.assertTrue(!p.start()&&p.a().getAmount()==1&&p.b().getAmount()==1,"Insufficient integer volumes cannot approximate an impossible target or lose fluid");h.succeed();
    }
    private static BlenderBlockEntity blender(GameTestHelper h){var pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(pos,BlenderContent.BLENDER.get().defaultBlockState());return (BlenderBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static FluidStack wine(int index,int amount,double abv,long age){var f=WineCatalog.sample(WineCatalog.varieties().get(index),amount);BatchProfile.of(f).withAlcoholFraction(abv).applyTo(f);return BarrelFluidData.withAge(f,age);}
    private static FluidStack fluid(String id,int amount){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)),amount);return ProductionInputs.defaultProfile(f).applyTo(f);}
    private static void tick(GameTestHelper h,BlenderBlockEntity p,int count){for(int i=0;i<count;i++)BlenderBlockEntity.tick(h.getLevel(),p.getBlockPos(),p.getBlockState(),p);}
}
