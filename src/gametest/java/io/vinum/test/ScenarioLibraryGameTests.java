package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.ModItems;
import io.vinum.production.ProductionInputs;
import io.vinum.test.harness.GameScenario;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Executable examples of the shared scenario DSL; no desktop automation or test-specific scripts. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class ScenarioLibraryGameTests {
    @GameTest(template = "barrel_space", timeoutTicks = 160)
    public static void scriptedWineScenarioUsesActionsWaitsAndAssertions(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        GameScenario.begin(helper)
                .place(pos, ModBlocks.CARBOY.get())
                .equip(new ItemStack(ModItems.GRAPE_JUICE_CHARDONNAY.get())).use(pos)
                .expectFluid(pos, ProductionInputs.id("juice_chardonnay"), 1000)
                .sneakUseEmptyHand(pos)
                .step("accelerate the fixture's frozen plan", scene -> {
                    var vessel = scene.vessel(pos);
                    helper.assertTrue(vessel.isFermenting(), "The actual block interaction must have started fermentation");
                    var saved = vessel.saveWithoutMetadata(helper.getLevel().registryAccess());
                    int duration = saved.getCompound("FermentationPlan").getInt("duration_ticks");
                    saved.putInt("FermentationProgress", Math.max(0, duration - 20));
                    vessel.loadWithComponents(saved, helper.getLevel().registryAccess());
                })
                .await("fermentation finishes", scene -> helper.assertTrue(!scene.vessel(pos).isFermenting(), "Still fermenting"))
                .equip(new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get())).use(pos)
                .expectHeld(ModItems.WINE_BOTTLE_CHARDONNAY.get(), 1)
                .expectFluid(pos, ProductionInputs.id("chardonnay"), 250)
                .succeed();
    }

    @GameTest(template = "barrel_space")
    public static void testCommandsAndStructuredReportingAreRegistered(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        helper.assertTrue(dispatcher.getRoot().getChild("vinumtest") != null, "World test commands must be available in the test mod");
        int listed = dispatcher.execute("vinumtest list", server.createCommandSourceStack().withSuppressedOutput());
        helper.assertTrue(listed >= 66, "The test command must discover existing reusable GameTests");
        helper.assertTrue(dispatcher.execute("vinumtest report", server.createCommandSourceStack().withSuppressedOutput()) == 1,
                "Structured result summary must be accessible in game");
        helper.succeed();
    }
}
