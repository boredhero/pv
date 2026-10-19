package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.fluid.ModFluids;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.production.BatchProfile;
import io.vinum.production.FermentationRecipe;
import io.vinum.production.ProductionInputs;
import net.minecraft.nbt.NbtOps;
import io.vinum.production.vessel.VesselMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class FermentationVesselGameTests {
    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final IFluidHandler.FluidAction EXECUTE = IFluidHandler.FluidAction.EXECUTE;
    private static final IFluidHandler.FluidAction SIMULATE = IFluidHandler.FluidAction.SIMULATE;

    @GameTest(template = "barrel_space")
    public static void carboyRejectsMustAndSimulationNeverChangesFluid(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(ORIGIN);
        helper.getLevel().setBlock(pos, ModBlocks.CARBOY.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        var carboy = vessel(helper, pos);
        var tank = carboy.getFluidHandler(null);
        FluidStack juice = new FluidStack(ModFluids.JUICE_CHARDONNAY.source().get(), 1000);
        FluidStack must = new FluidStack(ModFluids.MUST_CHARDONNAY.source().get(), 1000);
        helper.assertTrue(tank.fill(must, EXECUTE) == 0, "Glass carboys reject suspended must solids");
        helper.assertTrue(tank.fill(juice, SIMULATE) == 1000 && carboy.getLocalFluid().isEmpty(), "Simulated fill must not start fermentation or change volume");
        helper.assertTrue(tank.fill(juice, EXECUTE) == 1000, "Carboys accept clarified grape juice");
        helper.assertTrue(tank.drain(750, SIMULATE).getAmount() == 750 && carboy.getLocalFluid().getAmount() == 1000, "Simulated drain preserves all stored juice");
        helper.startSequence().thenIdle(21).thenExecute(() -> {
            helper.assertTrue(!carboy.isFermenting(), "Filled vessels wait for explicit start so players can finish loading");
            helper.assertTrue(tank.fill(juice, EXECUTE) == 1000, "A ready carboy remains fillable after a second");
            helper.assertTrue(carboy.startFermentation(), "Explicit start freezes the ready batch with spontaneous yeast");
            helper.assertTrue(!carboy.startFermentation() && tank.fill(juice, EXECUTE) == 0, "An active batch cannot start twice or accept new liquid");
        }).thenSucceed();
    }

    @GameTest(template = "barrel_space")
    public static void vatsExpandToThreeAndSplitConservesEveryLocalPortion(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        for (int size = 1; size <= 3; size++) {
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
                if (helper.getLevel().getBlockState(pos).is(ModBlocks.FERMENTATION_VAT.get())) continue;
                helper.getLevel().setBlock(pos, ModBlocks.FERMENTATION_VAT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            VesselMultiblock.reconcile(helper.getLevel(), origin.offset(size - 1, size - 1, size - 1));
            helper.assertTrue(vessel(helper, origin).getStructureSize() == size, "Complete vat cubes expand through all supported sizes");
        }
        var tank = vessel(helper, origin).getFluidHandler(null);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 27 * 16000), EXECUTE) == 432000, "Three-cube capacity is exactly 27 local reservoirs");
        BlockPos removedPos = origin.offset(1, 1, 1);
        var removed = vessel(helper, removedPos);
        var drops = Block.getDrops(removed.getBlockState(), helper.getLevel(), removedPos, removed);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1, "Harvested vat produces exactly one reservoir item");
        ItemStack drop = drops.getFirst();
        helper.getLevel().setBlock(removedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        int retained = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(2, 2, 2))) {
            if (pos.equals(removedPos)) continue;
            retained += vessel(helper, pos).getLocalFluid().getAmount();
        }
        helper.assertTrue(retained == 416000 && removed.getLocalFluid().getAmount() == 16000,
                "Removing one vat retains all 26 neighbors and only its own 16 litres");
        helper.assertTrue(drop.getMaxStackSize() == 1, "Filled vat drops cannot multiply through stacked placement");
        var restored = new FermentationVesselBlockEntity(removedPos, ModBlocks.FERMENTATION_VAT.get().defaultBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(drop.copy().get(DataComponents.BLOCK_ENTITY_DATA).copyTag(), helper.getLevel().registryAccess());
        helper.assertTrue(restored.getStructureSize() == 1 && restored.getLocalFluid().getAmount() == 16000,
                "Copied harvested data restores only its own portion and clears former assembly coordinates");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void frozenFermentationPlanPersistsAndRejectsActiveTransfers(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(ORIGIN);
        helper.getLevel().setBlock(pos, ModBlocks.CARBOY.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        var vessel = vessel(helper, pos);
        FluidStack juice = new FluidStack(ModFluids.JUICE_CHARDONNAY.source().get(), 1000);
        vessel.getFluidHandler(null).fill(juice, EXECUTE);
        // Loading a frozen short plan exercises the real server tick without a twenty-minute test.
        CompoundTag saved = vessel.saveWithFullMetadata(helper.getLevel().registryAccess());
        CompoundTag plan = new CompoundTag();
        plan.putString("input_fluid", "vinum:juice_chardonnay");
        plan.putString("output_fluid", "vinum:chardonnay");
        plan.putInt("duration_ticks", 100);
        plan.putDouble("sugar_consumed_g_per_litre", 200);
        plan.putDouble("alcohol_yield_fraction", .0006);
        plan.putDouble("solids_remaining_fraction", 0);
        saved.put("FermentationPlan", plan);
        saved.putInt("FermentationProgress", 40);
        vessel.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(vessel.isFermenting() && vessel.getProgress() == 40, "Frozen recipe and independent fermentation progress restore exactly");
        helper.assertTrue(vessel.getFluidHandler(null).fill(juice, EXECUTE) == 0 && vessel.getFluidHandler(null).drain(1000, EXECUTE).isEmpty(),
                "Active transfers reject without resetting or duplicating the batch");
        helper.assertTrue(vessel.addStarter(), "Optional starter can be added to an active batch");
        var drops = Block.getDrops(vessel.getBlockState(), helper.getLevel(), pos, vessel);
        helper.assertTrue(drops.size() == 1, "Active carboy has a single filled drop");
        var restored = new FermentationVesselBlockEntity(pos, ModBlocks.CARBOY.get().defaultBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(drops.getFirst().copy().get(DataComponents.BLOCK_ENTITY_DATA).copyTag(), helper.getLevel().registryAccess());
        helper.assertTrue(restored.getProgress() == 40 && restored.isFermenting()
                && FluidStack.isSameFluidSameComponents(restored.getLocalFluid(), vessel.getLocalFluid()),
                "Active dropped/copy-restored vessels retain culture, frozen plan, progress and intensive fluid components");
        helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(!vessel.isFermenting(), "Frozen recipe eventually finishes");
            helper.assertTrue(vessel.getFluid().getFluid().isSame(ModFluids.CHARDONNAY.source().get()) && vessel.getFluid().getAmount() == 1000,
                    "Fermentation changes beverage without changing volume");
        }).thenExecute(() -> {
            FluidStack finished = vessel.getLocalFluid();
            helper.assertTrue(!vessel.addStarter() && FluidStack.isSameFluidSameComponents(finished, vessel.getLocalFluid()),
                    "Finished wine rejects unnecessary starter without changing metadata");
        }).thenSucceed();
    }
    @GameTest(template = "barrel_space")
    public static void fermentedMustKeepsSkinsUntilPressedWithoutLosingWine(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(ORIGIN);
        helper.getLevel().setBlock(pos, ModBlocks.FERMENTATION_VAT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        var vat = vessel(helper, pos);
        FluidStack must = new FluidStack(ModFluids.MUST_MERLOT.source().get(), 1000);
        vat.getFluidHandler(null).fill(must, EXECUTE);
        helper.assertTrue(!vat.press(), "Raw must cannot skip fermentation by pressing");
        CompoundTag saved = vat.saveWithFullMetadata(helper.getLevel().registryAccess());
        FermentationRecipe plan = new FermentationRecipe(ProductionInputs.id("must_merlot"), ProductionInputs.id("merlot"), 40, 200, .0006, 1);
        saved.put("FermentationPlan", FermentationRecipe.CODEC.codec().encodeStart(NbtOps.INSTANCE, plan).getOrThrow());
        vat.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(!vat.press(), "Pressing never changes an active fermentation");
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(!vat.isFermenting(), "Must finishes its frozen fermentation plan"))
                .thenExecute(() -> {
                    FluidStack before = vat.getFluid();
                    BatchProfile profile = BatchProfile.of(before);
                    helper.assertTrue(before.getAmount() == 1000 && profile.solidsGramsPerLitre() == 100,
                            "Fermented red must retains all suspended skins");
                    BlockPos carboyPos = pos.offset(4, 0, 0);
                    helper.getLevel().setBlock(carboyPos, ModBlocks.CARBOY.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    helper.assertTrue(vessel(helper, carboyPos).getFluidHandler(null).fill(before, SIMULATE) == 0,
                            "Unpressed wine cannot enter a carboy");
                    helper.assertTrue(vat.press(), "Wine Press removes skins after fermentation");
                    FluidStack after = vat.getFluid();
                    BatchProfile pressed = BatchProfile.of(after);
                    helper.assertTrue(after.getAmount() == before.getAmount() && pressed.solidsGramsPerLitre() == 0
                                    && pressed.alcoholFraction() == profile.alcoholFraction()
                                    && pressed.ingredientProportions().equals(profile.ingredientProportions())
                                    && pressed.history().contains(ProductionInputs.id("pressing")),
                            "Pressing preserves liquid, alcohol, ingredients and records process history");
                    helper.assertTrue(!vat.press() && vessel(helper, carboyPos).getFluidHandler(null).fill(after, SIMULATE) == 1000,
                            "Pressing cannot repeat and clarified wine can enter a carboy");
                }).thenSucceed();
    }

    private static FermentationVesselBlockEntity vessel(GameTestHelper helper, BlockPos pos) {
        return (FermentationVesselBlockEntity) helper.getLevel().getBlockEntity(pos);
    }
}
