package io.vinum.test;

import io.vinum.bar.*;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.FluidBlending;
import io.vinum.blender.*;
import io.vinum.block.ModBlocks;
import io.vinum.item.*;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.*;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class NeutralSpiritGameTests {
    @GameTest(template="barrel_space")
    public static void allExistingSpiritsRectifyThroughTheStrengthStagesWithoutCreatingAlcohol(GameTestHelper h){
        for(String name:List.of("rum","honeyshine","brandy","tequila","golden_tequila")){
            var input=fluid(name,16000,.4);
            input.set(DataComponents.CUSTOM_NAME,Component.literal("Test batch"));
            input=BarrelFluidData.withAge(input,123456);
            for(double target:new double[]{.6,.8,.9,.95}){
                var recipe=recipe(h,input);
                int consumed=recipe.requiredInputAmount(input);
                var output=carboy().contents(recipe.output(input));
                h.assertTrue(is(output,"neutral_spirit")&&Math.abs(BatchProfile.of(output).alcoholFraction()-target)<1e-9,"Next rectification stage for "+name+" reaches "+target);
                var diluted=FluidBlending.blend(output.copyWithAmount(400),new FluidStack(Fluids.WATER,(int)Math.round(target*1000)-400));
                h.assertTrue(is(diluted,"vodka")&&NeutralSpiritProcess.refinementTooltip(diluted).equals("tooltip.vinum.rectification."+Math.round(target*100)),"Vodka retains highest refining history after dilution");
                h.assertTrue(consumed<=input.getAmount()&&alcohol(output)<=consumed*BatchProfile.of(input).alcoholFraction()+1e-8
                    &&consumed*BatchProfile.of(input).alcoholFraction()-alcohol(output)<1,"Whole-mB recipe never creates alcohol and rounding loss stays below1mB");
                h.assertTrue(BatchProfile.of(output).ingredientProportions().equals(BatchProfile.of(input).ingredientProportions())
                    &&Component.literal("Test batch").equals(output.get(DataComponents.CUSTOM_NAME))&&BarrelFluidData.getAge(output)==0,"Rectification retains provenance and unknown metadata, resetting maturation");
                input=output.copyWithAmount(16000);
            }
            h.assertTrue(recipes(h,input).isEmpty(),"95percent spirit cannot redistill indefinitely");
        }
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void invalidAndUnfermentedInputsCannotBypassRectification(GameTestHelper h){
        for(String name:List.of("cane_juice","mead_must","must_chardonnay"))h.assertTrue(recipes(h,fluid(name,16000,0)).isEmpty(),"Raw feedstock cannot rectify: "+name);
        h.assertTrue(recipes(h,fluid("rum",16000,0)).isEmpty(),"Explicit zero strength overrides defaults");
        for(double strength:new double[]{.95,.96,1})h.assertTrue(recipes(h,fluid("neutral_spirit",16000,strength)).isEmpty(),"Max-strength and stronger batches cannot manufacture more alcohol");
        var dirty=fluid("rum",16000,.6);var profile=BatchProfile.of(dirty);
        new BatchProfile(0,0,10,.6,profile.ingredientProportions(),profile.yeast(),profile.history()).applyTo(dirty);
        h.assertTrue(recipes(h,dirty).isEmpty(),"Suspended solids still require clarification");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void neutralDilutionMakesVodkaOnlyAtFortyAndConservesBatchMetadata(GameTestHelper h){
        var neutral=BarrelFluidData.withAge(fluid("neutral_spirit",400,.95),800);
        neutral.set(DataComponents.CUSTOM_NAME,Component.literal("Known origin"));
        var vodka=FluidBlending.blend(neutral,new FluidStack(Fluids.WATER,550));
        h.assertTrue(is(vodka,"vodka")&&vodka.getAmount()==950&&Math.abs(BatchProfile.of(vodka).alcoholFraction()-.4)<1e-9,"400mB95percent plus550water yields950mB40percent vodka");
        h.assertTrue(Math.abs(alcohol(neutral)-alcohol(vodka))<1e-9&&BarrelFluidData.getAge(vodka)==800
            &&BatchProfile.of(vodka).ingredientProportions().equals(BatchProfile.of(neutral).ingredientProportions())
            &&neutral.get(DataComponents.CUSTOM_NAME).equals(vodka.get(DataComponents.CUSTOM_NAME)),"Dilution retains alcohol, age, provenance and unknown components");
        h.assertTrue(is(FluidBlending.blend(neutral,new FluidStack(Fluids.WATER,100)),"neutral_spirit"),"Strong proofed neutral remains neutral");
        for(String name:List.of("rum","honeyshine","brandy","tequila","golden_tequila"))h.assertTrue(is(FluidBlending.blend(fluid(name,400,.95),new FluidStack(Fluids.WATER,550)),name),"Forty-percent "+name+" does not turn into vodka");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void blenderTargetAbvProducesVodkaAndPersistsTheReservedResult(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(pos,BlenderContent.BLENDER.get().defaultBlockState());
        var blender=(BlenderBlockEntity)h.getLevel().getBlockEntity(pos);
        blender.inputA().fill(fluid("neutral_spirit",400,.95),FluidAction.EXECUTE);blender.inputB().fill(new FluidStack(Fluids.WATER,550),FluidAction.EXECUTE);
        blender.selectMode(BlenderBlockEntity.TARGET_ABV);blender.setTargetAbv(400);
        h.assertTrue(blender.start(),"Target40percent can reserve vodka batch");
        var saved=blender.saveWithoutMetadata(h.getLevel().registryAccess());blender.loadWithComponents(saved,h.getLevel().registryAccess());
        for(int i=0;i<25;i++)BlenderBlockEntity.tick(h.getLevel(),pos,blender.getBlockState(),blender);
        h.assertTrue(is(blender.output(),"vodka")&&blender.output().getAmount()==950&&blender.a().isEmpty()&&blender.b().isEmpty(),"Reloaded pending batch commits exactly once");
        for(int i=0;i<25;i++)BlenderBlockEntity.tick(h.getLevel(),pos,blender.getBlockState(),blender);
        h.assertTrue(blender.output().getAmount()==950,"Idle ticks cannot duplicate vodka");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void realStillPausesForBlockedOutputAndResumesItsSavedRectificationPlan(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(4,1,4));
        var parts=List.of(ModBlocks.STEEL_BRAZIER.get(),ModBlocks.STEEL_POT.get(),ModBlocks.STEEL_COIL.get(),ModBlocks.STEEL_POT.get());
        for(int i=1;i<=4;i++)h.getLevel().setBlockAndUpdate(StillMultiblock.position(pos,Direction.NORTH,i),parts.get(i-1).defaultBlockState());
        StillMultiblock.build(h.getLevel(),pos);var still=(StillMasterTileEntity)h.getLevel().getBlockEntity(pos);
        still.toggleRectification();
        var input=fluid("neutral_spirit",4000,.9);var recipe=recipe(h,input);
        h.assertTrue(still.getFluidHandler(null).fill(input,FluidAction.EXECUTE)==4000,"Existing still accepts neutral spirit for final rectification");
        still.setItem(1,new ItemStack(carboy()));still.setItem(2,new ItemStack(Items.COAL,8));still.setItem(3,new ItemStack(Items.COBBLESTONE));
        for(int i=0;i<30;i++)StillMasterTileEntity.serverTick(h.getLevel(),pos,still.getBlockState(),still);
        h.assertTrue(still.getFluidHandler(null).getFluidInTank(0).getAmount()==4000&&still.getItem(2).getCount()==8,"Blocked output does not consume spirit or fuel");
        still.setItem(3,ItemStack.EMPTY);for(int i=0;i<20;i++)StillMasterTileEntity.serverTick(h.getLevel(),pos,still.getBlockState(),still);
        still.toggleRectification();h.assertTrue(still.rectification(),"Running rectification cannot change mode");
        var saved=still.saveWithoutMetadata(h.getLevel().registryAccess());still.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(still.rectification(),"Rectification mode survives saved plan reload");
        for(int i=0;i<recipe.durationTicks();i++)StillMasterTileEntity.serverTick(h.getLevel(),pos,still.getBlockState(),still);
        var result=carboy().contents(still.getItem(3));
        h.assertTrue(is(result,"neutral_spirit")&&Math.abs(BatchProfile.of(result).alcoholFraction()-.95)<1e-9
            &&still.getFluidHandler(null).getFluidInTank(0).getAmount()==4000-recipe.requiredInputAmount(input),"Saved dynamic rectification plan finishes one batch at95percent");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void bottlesServeNeutralAndVodkaIntoShotGlassesFromTheBar(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,1,3));h.getLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState());
        var bar=(BarBlockEntity)h.getLevel().getBlockEntity(pos);var glass=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        for(String name:List.of("neutral_spirit","vodka")){
            var bottle=(FluidContainerItem)BuiltInRegistries.ITEM.get(ProductionInputs.id(name+"_bottle"));
            var source=fluid(name,750,name.equals("vodka")?.4:.95);h.assertTrue(bar.insert(2,bottle.filled(source)),"New bottle places on bar");
            var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"neutral-serving"));
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(glass));
            bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0,1,0),Direction.UP,pos,false));
            var serving=glass.contents(player.getMainHandItem());
            h.assertTrue(is(serving,name)&&serving.getAmount()==50&&bottle.contents(bar.bottle(2)).getAmount()==700
                &&Math.abs(BatchProfile.of(serving).alcoholFraction()-BatchProfile.of(source).alcoholFraction())<1e-9,"Bar serving keeps identity and strength and consumes exactly50mB");
            h.assertTrue(glass.canDrink(player.getMainHandItem()),"New shot is drinkable");bar.remove(2);
        }
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void cleanFermentedInputsHaveAnExplicitRectificationRoute(GameTestHelper h){
        for(String name:List.of("fermented_cane_wash","mead","wine")){
            var input=fluid(name,16000,.12);var profile=BatchProfile.of(input);
            new BatchProfile(0,0,0,.12,profile.ingredientProportions(),profile.yeast(),profile.history()).applyTo(input);
            var recipe=recipe(h,input);
            var result=carboy().contents(recipe.output(input));
            h.assertTrue(is(result,"neutral_spirit")&&Math.abs(BatchProfile.of(result).alcoholFraction()-.6)<1e-9,"Clean fermented source can start at60percent neutral: "+name);
        }
        var vodka=fluid("vodka",500,.4);var neutral=fluid("neutral_spirit",500,.8);
        h.assertTrue(FluidBlending.canBlend(vodka,neutral),"Neutral family can blend across display identities");
        var mixed=FluidBlending.blend(vodka,neutral);
        h.assertTrue(is(mixed,"neutral_spirit")&&Math.abs(BatchProfile.of(mixed).alcoholFraction()-.6)<1e-9,"Vodka plus strong neutral reclassifies by actual60percent strength");h.succeed();
    }
    private static FluidContainerItem carboy(){return (FluidContainerItem)ModItems.JUICE_CARBOY.get();}
    private static double alcohol(FluidStack f){return f.getAmount()*BatchProfile.of(f).alcoholFraction();}
    private static boolean is(FluidStack f,String name){return !f.isEmpty()&&BuiltInRegistries.FLUID.getKey(f.getFluid()).equals(ProductionInputs.id(name));}
    private static FluidStack fluid(String name,int amount,double abv){var f=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(name)),amount);BatchProfile.of(f).withAlcoholFraction(abv).applyTo(f);return f;}
    private static List<DistillationRecipe> recipes(GameTestHelper h,FluidStack f){return h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream().map(r->r.value()).filter(r->r.matches(f)).toList();}
    private static DistillationRecipe recipe(GameTestHelper h,FluidStack f){return recipes(h,f).stream().filter(r->r.outputFluid().equals(ProductionInputs.id("neutral_spirit"))).findFirst().orElseThrow();}
}
