package io.vinum.test;

import io.vinum.production.*;
import io.vinum.sugar.*;
import io.vinum.block.ModBlocks;
import io.vinum.machine.WinePressBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class SugarProcessingGameTests {
    @GameTest(template = "barrel_space")
    public static void concentrationAndSugarSeparationConserveMassAcrossSaveReload(GameTestHelper h) {
        var plant = plant(h);
        var juice = fluid("cane_juice", 1000);
        double initialSugar = sugar(juice);
        h.assertTrue(plant.inputFluids().fill(juice, FluidAction.SIMULATE) == 1000 && plant.input().isEmpty(),
                "Input simulation cannot mutate the cane batch");
        plant.inputFluids().fill(juice, FluidAction.EXECUTE);
        h.assertTrue(!plant.start() && plant.input().getAmount() == 1000,
                "Concentration without fuel preserves the input");
        plant.inventory.setStackInSlot(SugarPlantBlockEntity.FUEL, new ItemStack(Items.COAL));
        h.assertTrue(plant.start(), "Fueled cane juice starts concentration");
        tick(h, plant, 100);
        var saved = plant.saveWithoutMetadata(h.getLevel().registryAccess());
        plant.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(plant.processing() && plant.progress() == 100,
                "In-flight concentration survives save/reload");
        h.assertTrue(plant.output().isEmpty(), "Unfinished concentration has no collectible output");
        tick(h, plant, 100);
        h.assertTrue(plant.output().getAmount() == 250 && Math.abs(sugar(plant.output()) - initialSugar) < 1e-8,
                "Evaporation reduces water volume without destroying or creating sugar");
        h.assertTrue(BatchProfile.of(plant.output()).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("cane_syrup"), 1.0)),
                "Concentration records syrup feedstock identity");
        plant.recycleOutput();
        plant.selectMode(1);
        h.assertTrue(plant.start(), "Built-in separation crystallizes the recycled syrup");
        tick(h, plant, 299);
        h.assertTrue(plant.inventory.getStackInSlot(SugarPlantBlockEntity.SOLID_OUTPUT).isEmpty() && plant.output().isEmpty(),
                "Crystals and molasses cannot be collected before both outputs are ready");
        tick(h, plant, 1);
        var crystals = plant.inventory.getStackInSlot(SugarPlantBlockEntity.SOLID_OUTPUT);
        var molasses = plant.output();
        double crystalSugar = crystals.get(ProductionDataComponents.BATCH_PROFILE.get()).sugarGramsPerLitre() * crystals.getCount();
        h.assertTrue(crystals.is(Items.SUGAR) && crystals.getCount() == 1
                && BuiltInRegistries.FLUID.getKey(molasses.getFluid()).equals(ProductionInputs.id("molasses"))
                && Math.abs(sugar(molasses) + crystalSugar - initialSugar) < 1e-8,
                "Sugar extraction yields vanilla sugar plus molasses without double-counting sugar");
        h.assertTrue(BatchProfile.of(molasses).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("molasses"), 1.0))
                && crystals.get(ProductionDataComponents.BATCH_PROFILE.get()).ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("cane_sugar"), 1.0)),
                "Separation records distinct molasses and cane-sugar feedstocks");
        h.assertTrue(plant.inventory.extractItem(SugarPlantBlockEntity.SOLID_OUTPUT, 64, false).getCount() == 1
                && plant.inventory.extractItem(SugarPlantBlockEntity.SOLID_OUTPUT, 64, false).isEmpty(), "Crystals are collected exactly once");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void panelaDissolvesWithProvenanceButOrdinarySugarDoesNot(GameTestHelper h) {
        var plant = plant(h);
        plant.inventory.setStackInSlot(SugarPlantBlockEntity.FUEL, new ItemStack(Items.COAL));
        var syrup = fluid("cane_syrup", 250);
        plant.inputFluids().fill(syrup, FluidAction.EXECUTE);
        plant.selectMode(2);
        h.assertTrue(plant.start(), "Whole syrup can be evaporated into panela");
        tick(h, plant, 300);
        var panela = plant.inventory.extractItem(SugarPlantBlockEntity.SOLID_OUTPUT, 64, false);
        h.assertTrue(plant.output().isEmpty() && panela.is(SugarContent.PANELA.get()) && panela.getCount() == 1,
                "Panela captures the complete syrup batch as one solid item");
        plant.inputFluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        plant.selectMode(4);
        plant.inventory.setStackInSlot(SugarPlantBlockEntity.INGREDIENT, new ItemStack(Items.SUGAR, 2));
        h.assertTrue(!plant.start() && plant.inventory.getStackInSlot(SugarPlantBlockEntity.INGREDIENT).is(Items.SUGAR),
                "Untracked honey/witch sugar cannot become cane rum through dissolution");
        plant.inventory.setStackInSlot(SugarPlantBlockEntity.INGREDIENT, panela);
        h.assertTrue(plant.start(), "Tracked panela starts dissolution");
        tick(h, plant, 20);
        h.assertTrue(Math.abs(sugar(plant.output()) - sugar(syrup)) < 1e-8,
                "Dissolution restores the original fermentable sugar mass");
        var profile = BatchProfile.of(plant.output());
        h.assertTrue(profile.ingredientProportions().equals(java.util.Map.of(ProductionInputs.id("panela"), 1.0))
                && profile.history().contains(ProductionInputs.id("dissolving")),
                "Dissolved panela retains cane provenance and processing history");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
                .stream().anyMatch(holder -> holder.value().matches(plant.output())),
                "Panela wash participates in the shared fermentation recipes");
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
        int bagasse = press.inventory.getStackInSlot(WinePressBlockEntity.EXTRA_OUTPUT_SLOT).getCount();
        h.assertTrue(press.inventory.getStackInSlot(WinePressBlockEntity.EXTRA_OUTPUT_SLOT).is(SugarContent.BAGASSE.get()),
                "Bagasse is stored in Extra Out rather than ejected into the world");
        h.assertTrue(juice.getAmount() == 1000 && press.inventory.getStackInSlot(0).isEmpty()
                && press.energy.getEnergyStored() == 0 && bagasse == 1,
                "One completed cane pressing consumes four stalks and 2000 FE and yields one litre plus one bagasse");
        h.assertTrue(Math.abs(sugar(juice) - 200) < 1e-8, "Pressed cane receives its fermentable sugar profile");
        h.succeed();
    }

    private static SugarPlantBlockEntity plant(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        h.getLevel().setBlockAndUpdate(pos, SugarContent.PLANT.get().defaultBlockState());
        return (SugarPlantBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static FluidStack fluid(String id, int amount) {
        var fluid = new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)), amount);
        return ProductionInputs.defaultProfile(fluid).applyTo(fluid);
    }
    private static double sugar(FluidStack fluid) { return BatchProfile.of(fluid).sugarGramsPerLitre() * fluid.getAmount() / 1000; }
    private static void tick(GameTestHelper h, SugarPlantBlockEntity plant, int ticks) {
        for (int i = 0; i < ticks; i++) SugarPlantBlockEntity.tick(h.getLevel(), plant.getBlockPos(), plant.getBlockState(), plant);
    }
}
