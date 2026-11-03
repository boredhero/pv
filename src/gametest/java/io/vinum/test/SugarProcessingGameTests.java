package io.vinum.test;

import io.vinum.production.*;
import io.vinum.sugar.*;
import io.vinum.block.ModBlocks;
import io.vinum.machine.WinePressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class SugarProcessingGameTests {
    @GameTest(template = "barrel_space")
    public static void concentrationAndSugarSeparationConserveMassAcrossSaveReload(GameTestHelper h) {
        var kettle = kettle(h);
        var juice = fluid("cane_juice", 1000);
        double initialSugar = sugar(juice);
        h.assertTrue(kettle.fluids().fill(juice, FluidAction.SIMULATE) == 1000 && kettle.contents().isEmpty(),
                "Kettle simulation cannot duplicate cane input");
        kettle.fluids().fill(juice, FluidAction.EXECUTE);
        h.assertTrue(kettle.start(), "Cane juice starts concentration");
        tick(h, kettle, 10);
        h.assertTrue(kettle.progress() == 0 && kettle.contents().getAmount() == 1000,
                "An unheated kettle neither evaporates nor advances");
        h.getLevel().setBlockAndUpdate(kettle.getBlockPos().below(), Blocks.CAMPFIRE.defaultBlockState());
        tick(h, kettle, 100);
        var saved = kettle.saveWithoutMetadata(h.getLevel().registryAccess());
        kettle.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(kettle.processing() && kettle.progress() == 100,
                "In-flight concentration survives save/reload");
        h.assertTrue(kettle.fluids().drain(1000, FluidAction.EXECUTE).isEmpty()
                && kettle.fluids().fill(juice, FluidAction.EXECUTE) == 0,
                "Inputs and outputs lock together while a batch is processing");
        tick(h, kettle, 100);
        h.assertTrue(kettle.contents().getAmount() == 250 && Math.abs(sugar(kettle.contents()) - initialSugar) < 1e-8,
                "Evaporation reduces water volume without destroying or creating sugar");
        h.assertTrue(BatchProfile.of(kettle.contents()).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("cane_syrup"), 1.0)),
                "Actual concentration records syrup feedstock identity");
        kettle.cycleMode();
        h.assertTrue(!kettle.start(), "Sugar extraction requires the separation insert");
        kettle.installInsert();
        h.assertTrue(kettle.start(), "Concentrated syrup crystallizes with an insert");
        tick(h, kettle, 299);
        h.assertTrue(kettle.solidOutput().isEmpty()
                && BuiltInRegistries.FLUID.getKey(kettle.contents().getFluid()).equals(ProductionInputs.id("cane_syrup")),
                "Crystals and molasses cannot be collected before both outputs are ready");
        tick(h, kettle, 1);
        var crystals = kettle.solidOutput();
        var molasses = kettle.contents();
        double crystalSugar = crystals.get(ProductionDataComponents.BATCH_PROFILE.get()).sugarGramsPerLitre() * crystals.getCount();
        h.assertTrue(crystals.is(Items.SUGAR) && crystals.getCount() == 1
                && BuiltInRegistries.FLUID.getKey(molasses.getFluid()).equals(ProductionInputs.id("molasses"))
                && Math.abs(sugar(molasses) + crystalSugar - initialSugar) < 1e-8,
                "Sugar extraction commits usable vanilla sugar plus residual molasses without double-counting sugar");
        h.assertTrue(BatchProfile.of(molasses).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("molasses"), 1.0))
                && crystals.get(ProductionDataComponents.BATCH_PROFILE.get()).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("cane_sugar"), 1.0)),
                "Actual separation records distinct molasses and cane-sugar feedstock identities");
        kettle.fluids().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        kettle.fluids().fill(fluid("cane_syrup", 250), FluidAction.EXECUTE);
        var before = kettle.contents();
        h.assertTrue(!kettle.start() && FluidStack.isSameFluidSameComponents(before, kettle.contents()),
                "Uncollected solid output blocks another batch atomically");
        h.assertTrue(kettle.takeOutput().getCount() == 1 && kettle.takeOutput().isEmpty(),
                "Crystals can be collected exactly once");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void panelaAndExtractedSugarDissolveWithProvenanceButOrdinarySugarDoesNot(GameTestHelper h) {
        var kettle = kettle(h);
        h.getLevel().setBlockAndUpdate(kettle.getBlockPos().below(), Blocks.CAMPFIRE.defaultBlockState());
        var syrup = fluid("cane_syrup", 250);
        kettle.fluids().fill(syrup, FluidAction.EXECUTE);
        kettle.cycleMode(); kettle.cycleMode();
        h.assertTrue(kettle.start(), "Whole syrup can be evaporated into panela without extracting molasses");
        tick(h, kettle, 300);
        var panela = kettle.takeOutput();
        h.assertTrue(kettle.contents().isEmpty() && panela.is(SugarContent.PANELA.get()) && panela.getCount() == 1,
                "Panela captures the complete syrup batch as one solid item");
        kettle.fluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        h.assertTrue(!kettle.dissolve(new ItemStack(Items.SUGAR)),
                "Untracked honey/witch sugar cannot become cane rum through dissolution");
        h.assertTrue(kettle.dissolve(panela) && Math.abs(sugar(kettle.contents()) - sugar(syrup)) < 1e-8,
                "Dissolving panela restores the original fermentable sugar mass");
        var profile = BatchProfile.of(kettle.contents());
        h.assertTrue(profile.ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("panela"), 1.0))
                && profile.history().contains(ProductionInputs.id("dissolving")),
                "Dissolved panela retains cane provenance and processing history");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
                .stream().anyMatch(holder -> holder.value().matches(kettle.contents())),
                "Panela wash participates in the real shared fermentation recipes");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void canePressConsumesFourStalksAndProducesBagasseOnce(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.WINE_PRESS.get().defaultBlockState());
        ModBlocks.WINE_PRESS.get().setPlacedBy(h.getLevel(), pos, h.getLevel().getBlockState(pos), null,
                new ItemStack(io.vinum.item.ModItems.WINE_PRESS.get()));
        var press = (WinePressBlockEntity) h.getLevel().getBlockEntity(pos);
        press.inventory.setStackInSlot(0, new ItemStack(Items.SUGAR_CANE, 4));
        for (int i = 0; i < 200; i++) WinePressBlockEntity.tick(h.getLevel(), pos, press.getBlockState(), press);
        h.assertTrue(press.fluids().getFluidInTank(0).isEmpty() && press.inventory.getStackInSlot(0).getCount() == 4,
                "Cane cannot produce juice without mechanical work");
        press.attachMotor(); press.energy.generate(2000);
        for (int i = 0; i < 400; i++) WinePressBlockEntity.tick(h.getLevel(), pos, press.getBlockState(), press);
        var juice = press.fluids().getFluidInTank(0);
        int bagasse = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
                .filter(item -> item.getItem().is(SugarContent.BAGASSE.get())).mapToInt(item -> item.getItem().getCount()).sum();
        h.assertTrue(juice.getAmount() == 1000 && press.inventory.getStackInSlot(0).isEmpty()
                && press.energy.getEnergyStored() == 0 && bagasse == 1,
                "One completed cane pressing consumes four stalks and 2000 FE and yields one litre plus one bagasse");
        h.assertTrue(Math.abs(sugar(juice) - 200) < 1e-8, "Pressed cane receives its fermentable sugar profile");
        h.succeed();
    }

    private static SugarKettleBlockEntity kettle(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, SugarContent.KETTLE.get().defaultBlockState());
        return (SugarKettleBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static FluidStack fluid(String id, int amount) {
        var fluid = new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)), amount);
        return ProductionInputs.defaultProfile(fluid).applyTo(fluid);
    }
    private static double sugar(FluidStack fluid) { return BatchProfile.of(fluid).sugarGramsPerLitre() * fluid.getAmount() / 1000; }
    private static void tick(GameTestHelper h, SugarKettleBlockEntity kettle, int ticks) {
        for (int i = 0; i < ticks; i++) SugarKettleBlockEntity.tick(h.getLevel(), kettle.getBlockPos(), kettle.getBlockState(), kettle);
    }
}
