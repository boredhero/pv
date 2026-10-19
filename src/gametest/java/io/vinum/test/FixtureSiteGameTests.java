package io.vinum.test;

import io.vinum.test.harness.FixtureSiteSelector;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class FixtureSiteGameTests {
    @GameTest(template = "barrel_space")
    public static void blockedInitialFixtureFindsAnotherSite(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 64, 0);
        BlockPos available = origin.offset(8, 16, -8);
        BlockPos result = FixtureSiteSelector.find(origin, -64, 320, available::equals);
        helper.assertTrue(result.equals(available), "Search must continue beyond blocked or unloaded candidates");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void fixtureSearchClampsToBuildCeiling(GameTestHelper helper) {
        BlockPos result = FixtureSiteSelector.find(new BlockPos(0, 319, 0), -64, 320, candidate -> {
            helper.assertTrue(candidate.getY() - 1 >= -64 && candidate.getY() + 3 < 320,
                    "Entire fixture must fit within build limits");
            return true;
        });
        helper.assertTrue(result.getY() == 316, "High players should get a valid fixture below the ceiling");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void unavailableFixtureSearchIsBounded(GameTestHelper helper) {
        AtomicInteger attempts = new AtomicInteger();
        try {
            FixtureSiteSelector.find(new BlockPos(0, 64, 0), -64, 320, candidate -> {
                attempts.incrementAndGet();
                return false;
            });
            throw new AssertionError("Unavailable space must fail without claiming a fixture");
        } catch (IllegalStateException expected) {
            helper.assertTrue(attempts.get() == 150 && expected.getMessage().contains("move to an open area"),
                    "Search must stop after bounded candidates with actionable diagnostics");
        }
        helper.succeed();
    }
}
