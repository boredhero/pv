package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.fluid.ModFluids;
import io.vinum.production.BatchProfile;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.production.vessel.VesselMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class ShallowVatGameTests {
    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);
    private static final IFluidHandler.FluidAction EXECUTE = IFluidHandler.FluidAction.EXECUTE;

    @GameTest(template = "barrel_space")
    public static void twoByTwoSingleLayerHasFourLocalReservoirs(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 1);
        VesselMultiblock.reconcile(helper.getLevel(), origin.offset(1, 0, 1));
        assertDimensions(helper, origin, 2, 1);
        var vat = vessel(helper, origin);
        helper.assertTrue(vat.getCapacity() == 64000 && vat.members().size() == 4, "Shallow 2x2 vat owns four reservoirs and exactly 64 litres");
        var tank = vessel(helper, origin.offset(1, 0, 1)).getFluidHandler(null);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 70000), IFluidHandler.FluidAction.SIMULATE) == 64000
                && vat.getFluid().isEmpty(), "Shallow fill simulation reports total capacity without mutation");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 70000), EXECUTE) == 64000, "A non-controller fills the whole shallow vessel");
        for (var part : vat.members()) helper.assertTrue(part.getLocalFluid().getAmount() == 16000, "Each shallow part retains its exact local share");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void threeByThreeShallowExpandsThroughIndependentHeights(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 3, 1);
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        assertDimensions(helper, origin, 3, 1);
        var vat = vessel(helper, origin);
        helper.assertTrue(vat.getCapacity() == 144000, "3x3x1 holds 144 litres");
        vat.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 144000), EXECUTE);
        for (int height = 2; height <= 3; height++) {
            place(helper, origin, 3, height);
            VesselMultiblock.reconcile(helper.getLevel(), origin.offset(2, height - 1, 2));
            assertDimensions(helper, origin, 3, height);
            helper.assertTrue(vat.getCapacity() == 144000 * height && vat.getFluid().getAmount() == 144000,
                    "Adding complete layers increases capacity while retaining all previously owned water");
        }
        helper.assertTrue(vat.getFluidHandler(null).fill(new FluidStack(Fluids.WATER, 400000), EXECUTE) == 288000
                && vat.getFluid().getAmount() == 432000, "3x3x3 still holds exactly 432 litres");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void shallowCandidatesCannotConsumeSlicesOfExistingGroups(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 2);
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        assertDimensions(helper, origin, 2, 2);
        place(helper, origin, 3, 1);
        VesselMultiblock.reconcile(helper.getLevel(), origin.offset(2, 0, 2));
        assertDimensions(helper, origin, 2, 2);
        helper.assertTrue(!vessel(helper, origin.offset(2, 0, 2)).getStructureOrigin().equals(origin),
                "A complete shallow footprint cannot steal only the bottom of a taller existing group");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void sameFluidWithDifferentProfilesRefusesShallowMerge(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 1);
        FluidStack juice = new FluidStack(ModFluids.JUICE_CHARDONNAY.source().get(), 1000);
        FluidStack cultured = BatchProfile.of(juice).withYeast(ResourceLocation.fromNamespaceAndPath("vinum", "wine_yeast")).applyTo(juice.copy());
        helper.assertTrue(!FluidStack.isSameFluidSameComponents(juice, cultured), "Profile rejection fixture contains distinct uncultured and cultured batches");
        helper.assertTrue(vessel(helper, origin).getFluidHandler(null).fill(juice, EXECUTE) == 1000
                && vessel(helper, origin.offset(1, 0, 1)).getFluidHandler(null).fill(cultured, EXECUTE) == 1000,
                "Both separate vats accept their complete distinct-profile fixtures");
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        helper.assertTrue(vessel(helper, origin).getStructureSize() == 1
                && FluidStack.isSameFluidSameComponents(vessel(helper, origin).getLocalFluid(), juice)
                && FluidStack.isSameFluidSameComponents(vessel(helper, origin.offset(1, 0, 1)).getLocalFluid(), cultured),
                "Same beverage with different intensive metadata stays separate with both profiles intact");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void activeShallowBatchRefusesExpansionAndPersistsHeight(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 1);
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        var vat = vessel(helper, origin);
        vat.getFluidHandler(null).fill(new FluidStack(ModFluids.JUICE_CHARDONNAY.source().get(), 64000), EXECUTE);
        helper.assertTrue(vat.startFermentation(), "A filled shallow vat can freeze a fermentation plan");
        CompoundTag saved = vat.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putInt("FermentationProgress", 40);
        vat.loadWithComponents(saved, helper.getLevel().registryAccess());
        place(helper, origin, 2, 2);
        VesselMultiblock.reconcile(helper.getLevel(), origin.offset(1, 1, 1));
        assertDimensions(helper, origin, 2, 1);
        helper.assertTrue(vat.isFermenting() && vat.saveWithFullMetadata(helper.getLevel().registryAccess()).getInt("FermentationProgress") == 40
                && vat.getFluid().getAmount() == 64000, "Rejected expansion retains shallow dimensions, raw progress, frozen plan and liquid");
        var restored = copy(helper, origin, vat.saveWithFullMetadata(helper.getLevel().registryAccess()));
        helper.assertTrue(restored.getStructureHeight() == 1 && restored.getStructureSize() == 2 && restored.isFermenting()
                && restored.saveWithFullMetadata(helper.getLevel().registryAccess()).getInt("FermentationProgress") == 40,
                "World save/reload retains independent shallow height and fermentation progress");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void shallowBreakAndItemReloadConserveExactOwnedFluid(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 1);
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        var vat = vessel(helper, origin);
        FluidStack juice = new FluidStack(ModFluids.JUICE_CHARDONNAY.source().get(), 57000);
        vat.getFluidHandler(null).fill(juice, EXECUTE);
        helper.assertTrue(vat.startFermentation(), "Shallow batch starts before split");
        BlockPos removedPos = origin.offset(1, 0, 1);
        var removed = vessel(helper, removedPos);
        CompoundTag saved = removed.saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.putInt("FermentationProgress", 40);
        removed.loadWithComponents(saved, helper.getLevel().registryAccess());
        FluidStack owned = removed.getLocalFluid();
        var drops = Block.getDrops(removed.getBlockState(), helper.getLevel(), removedPos, removed);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1 && owned.getAmount() == 9000,
                "The removed shallow corner drops exactly its own partially filled 9 litre reservoir");
        helper.getLevel().setBlock(removedPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        int retained = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(1, 0, 1))) {
            if (pos.equals(removedPos)) continue;
            var part = vessel(helper, pos);
            retained += part.getLocalFluid().getAmount();
            helper.assertTrue(part.getStructureSize() == 1 && part.getStructureHeight() == 1 && part.isFermenting(), "Split neighbors keep their own active plans and become singletons");
        }
        var restored = copy(helper, removedPos, drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA).copyTag());
        helper.assertTrue(retained == 48000 && restored.getLocalFluid().getAmount() == 9000
                && FluidStack.isSameFluidSameComponents(owned, restored.getLocalFluid())
                && restored.getStructureSize() == 1 && restored.getStructureHeight() == 1 && restored.isFermenting()
                && restored.saveWithFullMetadata(helper.getLevel().registryAccess()).getInt("FermentationProgress") == 40,
                "Drop reload clears assembly coordinates and conserves exact total fluid, profile and frozen progress");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void legacyCubeDataDefaultsHeightToStoredWidth(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        place(helper, origin, 2, 2);
        VesselMultiblock.reconcile(helper.getLevel(), origin);
        CompoundTag saved = vessel(helper, origin).saveWithFullMetadata(helper.getLevel().registryAccess());
        saved.remove("StructureHeight");
        var restored = copy(helper, origin, saved);
        helper.assertTrue(restored.getStructureSize() == 2 && restored.getStructureHeight() == 2 && restored.getCapacity() == 128000,
                "Legacy cube saves restore their original height and capacity");
        helper.succeed();
    }

    private static void place(GameTestHelper helper, BlockPos origin, int width, int height) {
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, height - 1, width - 1))) {
            if (!helper.getLevel().getBlockState(pos).is(ModBlocks.FERMENTATION_VAT.get()))
                helper.getLevel().setBlock(pos, ModBlocks.FERMENTATION_VAT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
    private static void assertDimensions(GameTestHelper helper, BlockPos origin, int width, int height) {
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, height - 1, width - 1))) {
            var part = vessel(helper, pos);
            helper.assertTrue(part.getStructureOrigin().equals(origin) && part.getStructureSize() == width && part.getStructureHeight() == height,
                    "Every member shares the expected origin and independent width/height");
        }
    }
    private static FermentationVesselBlockEntity copy(GameTestHelper helper, BlockPos pos, CompoundTag saved) {
        var restored = new FermentationVesselBlockEntity(pos, ModBlocks.FERMENTATION_VAT.get().defaultBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        return restored;
    }
    private static FermentationVesselBlockEntity vessel(GameTestHelper helper, BlockPos pos) {
        return (FermentationVesselBlockEntity) helper.getLevel().getBlockEntity(pos);
    }
}
