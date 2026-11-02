package io.vinum.test;

import io.vinum.barrel.*;
import io.vinum.beverage.*;
import io.vinum.item.*;
import io.vinum.production.*;
import io.vinum.fluid.ModFluids;
import io.vinum.block.ModBlocks;
import io.vinum.tileentity.BarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BeverageContainerGameTests {
    private static FluidStack wine(String name, int amount) {
        return WineCatalog.sample(WineCatalog.varieties().stream().filter(v -> v.legacyFluid().getPath().equals(name)).findFirst().orElseThrow(), amount);
    }
    private static void near(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 1e-8, message + ": " + actual);
    }
    @GameTest(template="barrel_space")
    public static void partialFillBlendsAndConservesComponents(GameTestHelper h) {
        ItemStack glass = new ItemStack(ModItems.WINE_GLASS_EMPTY.get());
        var handler = glass.getCapability(Capabilities.FluidHandler.ITEM);
        h.assertTrue(handler != null && handler.getTankCapacity(0) == 125, "Glass exposes its 125 mB capability");
        FluidStack first = BarrelFluidData.withAge(wine("merlot", 50), 400);
        FluidStack second = BarrelFluidData.withAge(wine("chardonnay", 100), 100);
        BatchProfile.of(second).withAlcoholFraction(.08).applyTo(second);
        h.assertTrue(handler.fill(first, FluidAction.SIMULATE) == 50 && handler.getFluidInTank(0).isEmpty(), "Simulation cannot fill the glass");
        h.assertTrue(handler.fill(first, FluidAction.EXECUTE) == 50, "Partial glass accepts 50");
        h.assertTrue(handler.fill(second, FluidAction.EXECUTE) == 75 && second.getAmount() == 100, "Only available capacity is accepted, input remains immutable");
        FluidStack full = handler.getFluidInTank(0);
        h.assertTrue(full.getFluid() == ModFluids.WINE.source().get() && full.getAmount() == 125, "One canonical fluid holds the blend");
        near(h, BatchProfile.of(full).ingredientProportions().get(ProductionInputs.id("grapes_merlot")), .4, "Volume-weighted merlot share");
        near(h, BatchProfile.of(full).alcoholFraction(), .096, "Volume-weighted ABV");
        h.assertTrue(BarrelFluidData.getAge(full) == 220, "Volume-weighted maturation age");
        FluidStack split = handler.drain(25, FluidAction.EXECUTE);
        h.assertTrue(split.getAmount() == 25 && handler.getFluidInTank(0).getAmount() == 100, "Drain conserves amount");
        h.assertTrue(BatchProfile.of(split).equals(BatchProfile.of(full)), "Splits retain intensive composition");
        var loaded = ItemStack.parseOptional(h.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag)handler.getContainer().save(h.getLevel().registryAccess()));
        h.assertTrue(FluidStack.matches(loaded.getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0), handler.getFluidInTank(0)), "Save/load retains all contents");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void legacyGlassMigratesWithoutLosingWine(GameTestHelper h) {
        ItemStack old = new ItemStack(ModItems.WINE_GLASS_MERLOT.get());
        var handler = old.getCapability(Capabilities.FluidHandler.ITEM);
        FluidStack sample = handler.drain(25, FluidAction.SIMULATE);
        h.assertTrue(sample.getAmount() == 25 && handler.getContainer().is(ModItems.WINE_GLASS_MERLOT.get()), "Simulation preserves legacy item");
        near(h, BatchProfile.of(sample).alcoholFraction(), .12, "Legacy wine gets its historical default ABV");
        handler.drain(25, FluidAction.EXECUTE);
        h.assertTrue(handler.getContainer().is(ModItems.WINE_GLASS_EMPTY.get()) && handler.getFluidInTank(0).getAmount() == 100, "Mutation upgrades to canonical container without losing remainder");
        h.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 25), FluidAction.EXECUTE) == 0, "Unsupported liquids are rejected by the acceptance tag");
        h.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 25), FluidAction.EXECUTE) == 0, "Unspecified cross-family mixing is rejected");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void carboyMatchesPlacedCapacityAndPreservesPartialJuice(GameTestHelper h) {
        ItemStack old = new ItemStack(ModItems.GRAPE_JUICE_MERLOT.get());
        var handler = old.getCapability(Capabilities.FluidHandler.ITEM);
        h.assertTrue(handler.getTankCapacity(0) == io.vinum.production.vessel.FermentationVesselBlockEntity.CARBOY_CAPACITY, "Portable and placed carboys agree");
        h.assertTrue(handler.getFluidInTank(0).getAmount() == 1000, "Legacy juice contains one litre, not a full carboy");
        FluidStack juice = handler.getFluidInTank(0).copyWithAmount(5000);
        h.assertTrue(handler.fill(juice, FluidAction.EXECUTE) == 3000, "Top-up accepts only three litres");
        h.assertTrue(handler.getContainer().is(ModItems.JUICE_CARBOY.get()), "Juice migrates to reusable carboy");
        handler.drain(125, FluidAction.EXECUTE);
        h.assertTrue(handler.getFluidInTank(0).getAmount() == 3875, "Partial pour retains remainder");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void barrelBlendingPreservesExistingBatch(GameTestHelper h) {
        var pos = new BlockPos(2,2,2);
        h.setBlock(pos, ModBlocks.BARREL.get());
        var barrel = (BarrelBlockEntity)h.getBlockEntity(pos);
        var tank = barrel.getFluidHandler(null);
        tank.fill(wine("merlot", 1000), FluidAction.EXECUTE);
        h.assertTrue(tank.fill(wine("chardonnay", 3000), FluidAction.SIMULATE) == 3000 && tank.getFluidInTank(0).getAmount() == 1000, "Blend simulation cannot mutate barrel");
        tank.fill(wine("chardonnay", 3000), FluidAction.EXECUTE);
        FluidStack result = tank.drain(125, FluidAction.EXECUTE);
        near(h, BatchProfile.of(result).ingredientProportions().get(ProductionInputs.id("grapes_merlot")), .25, "Barrel retains both wines, weighted by volume");
        h.assertTrue(tank.getFluidInTank(0).getAmount() == 3875, "Barrel drain conserves volume");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void unknownMetadataPreventsLossyBlendAndFractionalAlcoholAccumulates(GameTestHelper h) {
        FluidStack a=wine("merlot",125), b=wine("chardonnay",125);
        a.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Addon marker"));
        h.assertTrue(!FluidBlending.canBlend(a,b), "Unknown differing metadata must not be silently discarded");
        var bac = new io.vinum.capability.BAC();
        h.assertTrue(bac.acceptAlcohol(7.5) == 0, "Half serving retains fractional dose");
        var loaded = new io.vinum.capability.BAC();
        loaded.deserializeNBT(h.getLevel().registryAccess(),bac.serializeNBT(h.getLevel().registryAccess()));
        h.assertTrue(loaded.acceptAlcohol(7.5) == 1, "Two half servings count even across saves");
        h.succeed();
    }
}
