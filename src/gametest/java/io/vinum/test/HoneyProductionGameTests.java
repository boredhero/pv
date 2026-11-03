package io.vinum.test;

import io.vinum.bar.*;
import io.vinum.barrel.*;
import io.vinum.beverage.*;
import io.vinum.block.ModBlocks;
import io.vinum.item.*;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.*;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.sugar.*;
import io.vinum.tileentity.*;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

/** End-to-end honey preparation, fermentation, distilled spirits, maturation and serving. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class HoneyProductionGameTests {
    @GameTest(template="barrel_space")
    public static void coldKettlePreparesMeadMustAndReturnsExactlyOneHoneyBottle(GameTestHelper h) {
        var kettle=kettle(h,new BlockPos(2,1,2));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));
        use(h,kettle,player);
        h.assertTrue(kettle.contents().getAmount()==1000 && player.getMainHandItem().is(Items.BUCKET),
                "Vanilla water bucket fills the cold kettle and returns its bucket");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.HONEY_BOTTLE,2));
        use(h,kettle,player);
        var must=kettle.contents();
        h.assertTrue(is(must,"mead_must") && must.getAmount()==1250
                && Math.abs(BatchProfile.of(must).sugarGramsPerLitre()-200)<1e-9,
                "One honey bottle and one litre water prepare1250mB of200g/L honey must without heating");
        h.assertTrue(player.getMainHandItem().is(Items.HONEY_BOTTLE) && player.getMainHandItem().getCount()==1
                && player.getInventory().countItem(Items.GLASS_BOTTLE)==1,
                "Preparation consumes exactly one honey bottle and returns exactly one glass bottle");
        h.assertTrue(BatchProfile.of(must).ingredientProportions().keySet().stream().anyMatch(key->key.getPath().equals("honey")),
                "Must records honey provenance rather than cane sugar");
        var restored=new SugarKettleBlockEntity(kettle.getBlockPos(),kettle.getBlockState());
        restored.loadWithComponents(kettle.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(FluidStack.isSameFluidSameComponents(restored.contents(),must)
                && restored.contents().getAmount()==1250,"Prepared honey batch survives kettle serialization");
        h.succeed();
    }

    @GameTest(template="barrel_space")
    public static void honeyPreparationRejectsInvalidOrFullBatchesWithoutConsumption(GameTestHelper h) {
        var kettle=kettle(h,new BlockPos(2,1,2));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.HONEY_BOTTLE));
        use(h,kettle,player);
        h.assertTrue(kettle.contents().isEmpty() && player.getMainHandItem().is(Items.HONEY_BOTTLE),
                "Honey cannot prepare must without water");
        for(var input:List.of(new FluidStack(Fluids.WATER,4000),fluid("cane_juice",1000))) {
            kettle.fluids().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
            kettle.fluids().fill(input,FluidAction.EXECUTE);
            var before=kettle.contents();
            use(h,kettle,player);
            h.assertTrue(FluidStack.isSameFluidSameComponents(before,kettle.contents())
                    && before.getAmount()==kettle.contents().getAmount()
                    && player.getMainHandItem().is(Items.HONEY_BOTTLE)
                    && player.getInventory().countItem(Items.GLASS_BOTTLE)==0,
                    "Full water or unrelated cane batches reject honey atomically");
        }
        kettle.fluids().drain(Integer.MAX_VALUE,FluidAction.EXECUTE);
        kettle.fluids().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
        var creative=h.makeMockPlayer(GameType.CREATIVE);creative.getAbilities().instabuild=true;
        creative.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.HONEY_BOTTLE));
        use(h,kettle,creative);
        h.assertTrue(is(kettle.contents(),"mead_must") && kettle.contents().getAmount()==1250
                && creative.getMainHandItem().is(Items.GLASS_BOTTLE),
                "Creative preparation also consumes one ingredient and returns its glass container");
        h.succeed();
    }

    @GameTest(template="barrel_space",timeoutTicks=60)
    public static void honeyMustFermentsInVatAndAirlockedCarboyWithCleanDrinkableOutput(GameTestHelper h) {
        var carboy=vessel(h,new BlockPos(2,1,2),ModBlocks.CARBOY.get());
        var vat=vessel(h,new BlockPos(6,1,2),ModBlocks.FERMENTATION_VAT.get());
        var must=BarrelFluidData.withAge(fluid("mead_must",3750),123456);
        var recipe=fermentation(h,must);
        for(var vessel:List.of(carboy,vat))vessel.getFluidHandler(null).fill(must,FluidAction.EXECUTE);
        h.assertTrue(!carboy.startFermentation(),"A carboy requires an airlock for honey fermentation");
        carboy.installAirlock();
        for(var vessel:List.of(carboy,vat)) {
            h.assertTrue(vessel.startFermentation(),"Both existing fermenters accept honey must");
            var saved=vessel.saveWithoutMetadata(h.getLevel().registryAccess());
            saved.putInt("FermentationProgress",recipe.durationTicks()-20);
            vessel.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(vessel.getFluidHandler(null).drain(50,FluidAction.EXECUTE).isEmpty(),
                    "A running mead fermentation locks extraction");
        }
        h.runAfterDelay(21,()->{
            for(var vessel:List.of(carboy,vat)) {
                var mead=vessel.getFluid();var profile=BatchProfile.of(mead);
                h.assertTrue(is(mead,"mead") && mead.getAmount()==3750 && !vessel.isFermenting(),
                        "Real vessel ticks finish the persisted honey fermentation plan");
                h.assertTrue(Math.abs(profile.alcoholFraction()-.12)<1e-9 && profile.solidsGramsPerLitre()==0
                        && profile.ingredientProportions().equals(BatchProfile.of(must).ingredientProportions()),
                        "Clean honey must yields12percent mead with retained honey provenance");
                h.assertTrue(BarrelFluidData.getAge(mead)<100,"Fermentation resets prior maturation age");
                var glass=(FluidContainerItem)ModItems.WINE_GLASS_EMPTY.get();
                h.assertTrue(glass.canDrink(glass.filled(mead.copyWithAmount(125))),
                        "Finished clean mead can be served without a fictitious straining requirement");
            }
            h.succeed();
        });
    }

    @GameTest(template="barrel_space",timeoutTicks=50)
    public static void meadMaturesInSealedCarboyAndBarrelButNotOpenCarboy(GameTestHelper h) {
        var mead=fluid("mead",750);
        var aging=BeverageCatalog.lookupFluid(mead.getFluid()).orElseThrow().aging();
        long threshold=aging.stages().get(1).minimumTicks().getAsLong();
        var near=BarrelFluidData.withAge(mead,threshold-2);
        var sealed=vessel(h,new BlockPos(2,1,2),ModBlocks.CARBOY.get());sealed.installAirlock();
        var open=vessel(h,new BlockPos(5,1,2),ModBlocks.CARBOY.get());
        var pos=h.absolutePos(new BlockPos(8,1,2));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.BARREL.get().defaultBlockState());
        var barrel=(BarrelBlockEntity)h.getLevel().getBlockEntity(pos);
        sealed.getFluidHandler(null).fill(near,FluidAction.EXECUTE);
        open.getFluidHandler(null).fill(near,FluidAction.EXECUTE);
        barrel.getFluidHandler(null).fill(near,FluidAction.EXECUTE);
        h.runAfterDelay(21,()->{
            h.assertTrue(BarrelFluidData.getAge(sealed.getFluid())>=threshold
                    && BarrelFluidData.getAge(barrel.getFluidHandler(null).getFluidInTank(0))>=threshold
                    && BarrelFluidData.getAge(open.getFluid())==threshold-2,
                    "Mead matures in its sealed glass or barrel route, while open glass does not advance");
            h.assertTrue(aging.stage(BarrelFluidData.getAge(sealed.getFluid())).equals("tooltip.vinum.maturation.mead_mature")
                    && Math.abs(BatchProfile.of(sealed.getFluid()).alcoholFraction()-.12)<1e-9,
                    "Mature mead changes stage without creating alcohol");
            h.succeed();
        });
    }

    @GameTest(template="barrel_space")
    public static void fermentedHoneyDistillsIntoHoneyshineWithoutCreatingAlcoholOrInheritingAge(GameTestHelper h) {
        var must=fluid("mead_must",4000);
        var mead=fermentation(h,must).output(must);
        verifyDistillation(h,BarrelFluidData.withAge(mead,9999999),"honeyshine");
        h.succeed();
    }

    @GameTest(template="barrel_space")
    public static void cleanWineDistillsIntoBrandyWithVarietyPreserved(GameTestHelper h) {
        var wine=WineCatalog.sample(WineCatalog.varieties().getFirst(),4000);
        verifyDistillation(h,BarrelFluidData.withAge(wine,9999999),"brandy");
        var must=fluid("must_chardonnay",4000);
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .noneMatch(r->r.value().matches(must)),"Raw grape must cannot bypass fermentation and clarification into brandy");
        var unstrained=wine.copy();var profile=BatchProfile.of(unstrained);
        new BatchProfile(profile.sugarGramsPerLitre(),profile.starchGramsPerLitre(),20,
                profile.alcoholFraction(),profile.ingredientProportions(),profile.yeast(),profile.history()).applyTo(unstrained);
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .noneMatch(r->r.value().matches(unstrained)),"Fermented wine with suspended grape solids must be strained before distilling");
        h.succeed();
    }

    @GameTest(template="barrel_space")
    public static void honeyAndBrandyBottlesAreCraftableAndServeTheirDrinksOnTheBar(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));
        h.getLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState());
        var bar=(BarBlockEntity)h.getLevel().getBlockEntity(pos);
        for(String name:List.of("mead","honeyshine","brandy")) {
            var bottle=(FluidContainerItem)BuiltInRegistries.ITEM.get(ProductionInputs.id(name+"_bottle"));
            h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .anyMatch(r->r.value().getResultItem(h.getLevel().registryAccess()).is(bottle)),
                    "Each new bottle has a loaded survival crafting recipe: "+name);
            var liquid=fluid(name,750);var aging=BeverageCatalog.lookupFluid(liquid.getFluid()).orElseThrow().aging();
            long age=aging.stages().getLast().minimumTicks().getAsLong();
            liquid=BarrelFluidData.withAge(liquid,age);
            if(!name.equals("mead"))BatchProfile.of(liquid).withAlcoholFraction(.4).applyTo(liquid);
            h.assertTrue(bar.insert(2,bottle.filled(liquid)),"New bottled beverage is accepted by the bar: "+name);
            var glass=(FluidContainerItem)(name.equals("mead")?ModItems.WINE_GLASS_EMPTY.get():ModItems.SHOT_GLASS.get());
            var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"honey-serving"));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(glass));
            var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0,1,0),Direction.UP,pos,false);
            bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
            var serving=glass.contents(player.getMainHandItem());int expected=name.equals("mead")?125:50;
            h.assertTrue(serving.getAmount()==expected && bottle.contents(bar.bottle(2)).getAmount()==750-expected
                    && BarrelFluidData.getAge(serving)==age,"Serving conserves liquid and maturation: "+name);
            var empty=glass.finishUsingItem(player.getMainHandItem(),h.getLevel(),player);
            h.assertTrue(empty.is(glass) && glass.contents(empty).isEmpty()
                    && player.getData(io.vinum.capability.BACCapability.BAC_STATE).getBACLevel()>0,
                    "Serving drinks normally and returns the reusable glass: "+name);
            bar.remove(2);
        }
        h.succeed();
    }

    @GameTest(template="barrel_space",timeoutTicks=50)
    public static void honeyshineAndBrandyNeedWoodForLightAndDarkAging(GameTestHelper h) {
        var barrels=new java.util.ArrayList<BarrelBlockEntity>();
        var carboys=new java.util.ArrayList<FermentationVesselBlockEntity>();
        var thresholds=new java.util.ArrayList<Long>();
        int index=0;
        for(String name:List.of("honeyshine","brandy")) {
            var spirit=fluid(name,750);
            var aging=BeverageCatalog.lookupFluid(spirit.getFluid()).orElseThrow().aging();
            long light=aging.stages().get(1).minimumTicks().getAsLong();
            long dark=aging.stages().get(2).minimumTicks().getAsLong();
            h.assertTrue(light>0&&dark>light
                    &&aging.stage(light-1).equals("tooltip.vinum.maturation."+name+"_white")
                    &&aging.stage(light).equals("tooltip.vinum.maturation."+name+"_light_aged")
                    &&aging.stage(dark).equals("tooltip.vinum.maturation."+name+"_dark_aged"),
                    "Distinct light and dark maturation thresholds exist for "+name);
            for(long threshold:List.of(light,dark)) {
                var near=BarrelFluidData.withAge(spirit,threshold-2);
                var pos=h.absolutePos(new BlockPos(2+index*2,1,2));
                h.getLevel().setBlockAndUpdate(pos,ModBlocks.BARREL.get().defaultBlockState());
                var barrel=(BarrelBlockEntity)h.getLevel().getBlockEntity(pos);
                barrel.getFluidHandler(null).fill(near,FluidAction.EXECUTE);barrels.add(barrel);
                var carboy=vessel(h,new BlockPos(2+index*2,1,5),ModBlocks.CARBOY.get());
                carboy.installAirlock();carboy.getFluidHandler(null).fill(near,FluidAction.EXECUTE);
                carboys.add(carboy);thresholds.add(threshold);index++;
            }
        }
        h.runAfterDelay(21,()->{
            for(int i=0;i<barrels.size();i++) {
                var aged=barrels.get(i).getFluidHandler(null).getFluidInTank(0);
                h.assertTrue(BarrelFluidData.getAge(aged)>=thresholds.get(i)
                        &&BarrelFluidData.getAge(carboys.get(i).getFluid())==thresholds.get(i)-2,
                        "Spirit advances in wood but cannot obtain barrel maturation in glass");
                h.assertTrue(aged.getAmount()==750&&Math.abs(BatchProfile.of(aged).alcoholFraction()-.6)<1e-9,
                        "Spirit aging preserves volume and strength");
            }
            h.succeed();
        });
    }

    private static void verifyDistillation(GameTestHelper h,FluidStack input,String outputName) {
        var recipe=h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .map(holder->holder.value()).filter(r->r.matches(input)&&r.outputFluid().equals(ProductionInputs.id(outputName))).findFirst().orElseThrow();
        var pos=h.absolutePos(new BlockPos(4,1,4));
        Block[] parts={ModBlocks.STEEL_BRAZIER.get(),ModBlocks.STEEL_POT.get(),ModBlocks.STEEL_COIL.get(),ModBlocks.STEEL_POT.get()};
        for(int i=1;i<=4;i++)h.getLevel().setBlockAndUpdate(StillMultiblock.position(pos,Direction.NORTH,i),parts[i-1].defaultBlockState());
        StillMultiblock.build(h.getLevel(),pos);var still=(StillMasterTileEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(still.getFluidHandler(null).fill(input,FluidAction.EXECUTE)==input.getAmount(),"Still accepts the actual finished fermented beverage");
        still.setItem(1,new ItemStack(ModItems.JUICE_CARBOY.get()));still.setItem(2,new ItemStack(Items.COAL));
        for(int i=0;i<recipe.durationTicks();i++)StillMasterTileEntity.serverTick(h.getLevel(),pos,still.getBlockState(),still);
        var output=still.getItem(3);
        h.assertTrue(output.is(ModItems.JUICE_CARBOY.get()),"Distillation collects the spirit in a bulk carboy");
        var spirit=((FluidContainerItem)output.getItem()).contents(output);
        h.assertTrue(is(spirit,outputName)&&spirit.getAmount()==750&&Math.abs(BatchProfile.of(spirit).alcoholFraction()-.6)<1e-9,
                "Still produces750mB at60percent: "+outputName);
        double consumed=alcohol(input)-alcohol(still.getFluidHandler(null).getFluidInTank(0));
        h.assertTrue(Math.abs(consumed-alcohol(spirit))<.2&&BarrelFluidData.getAge(spirit)==0
                &&BatchProfile.of(spirit).ingredientProportions().equals(BatchProfile.of(input).ingredientProportions()),
                "Distillation conserves alcohol and provenance while resetting maturation");
        var proofed=FluidBlending.blend(spirit,new FluidStack(Fluids.WATER,375));
        h.assertTrue(proofed.getAmount()==1125&&Math.abs(BatchProfile.of(proofed).alcoholFraction()-.4)<1e-9
                &&Math.abs(alcohol(proofed)-alcohol(spirit))<1e-9,"Proofing to40percent preserves all distilled alcohol");
    }
    private static double alcohol(FluidStack fluid){return fluid.getAmount()*BatchProfile.of(fluid).alcoholFraction();}
    private static boolean is(FluidStack fluid,String id){return !fluid.isEmpty()&&BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(ProductionInputs.id(id));}
    private static FluidStack fluid(String id,int amount){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)),amount);return ProductionInputs.defaultProfile(f).applyTo(f);}
    private static FermentationRecipe fermentation(GameTestHelper h,FluidStack fluid){return h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream().map(holder->holder.value()).filter(r->r.matches(fluid)).findFirst().orElseThrow();}
    private static FermentationVesselBlockEntity vessel(GameTestHelper h,BlockPos relative,Block block){var pos=h.absolutePos(relative);h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());return (FermentationVesselBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static SugarKettleBlockEntity kettle(GameTestHelper h,BlockPos relative){var pos=h.absolutePos(relative);h.getLevel().setBlockAndUpdate(pos,SugarContent.KETTLE.get().defaultBlockState());return (SugarKettleBlockEntity)h.getLevel().getBlockEntity(pos);}
    private static void use(GameTestHelper h,SugarKettleBlockEntity kettle,Player player){var pos=kettle.getBlockPos();kettle.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
}
