package io.vinum.test.harness;

import io.vinum.production.vessel.FermentationVesselBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Reusable world-test DSL. Actions use server interaction dispatch, not real-client input.
 * Positions are relative to the GameTest structure. Await retries assertions until the test timeout.
 */
public final class GameScenario {
    private final GameTestHelper helper;
    private final GameTestSequence sequence;
    private final Player actor;

    private GameScenario(GameTestHelper helper) {
        this.helper = helper;
        sequence = helper.startSequence();
        actor = helper.makeMockPlayer(GameType.SURVIVAL);
    }
    public static GameScenario begin(GameTestHelper helper) { return new GameScenario(helper); }
    public GameTestHelper helper() { return helper; }
    public Player actor() { return actor; }

    public GameScenario step(String name, Consumer<GameScenario> action) {
        sequence.thenExecute(() -> named(name, () -> action.accept(this)));
        return this;
    }
    public GameScenario await(String name, Consumer<GameScenario> assertion) {
        sequence.thenWaitUntil(() -> named(name, () -> assertion.accept(this)));
        return this;
    }
    public GameScenario waitTicks(int ticks) { sequence.thenIdle(ticks); return this; }
    public void succeed() { sequence.thenSucceed(); }

    public GameScenario place(BlockPos pos, Block block) {
        return step("place " + BuiltInRegistries.BLOCK.getKey(block), scene -> helper.setBlock(pos, block));
    }
    public GameScenario equip(ItemStack stack) {
        ItemStack snapshot = stack.copy();
        return step("equip " + snapshot, scene -> actor.setItemInHand(InteractionHand.MAIN_HAND, snapshot.copy()));
    }
    public GameScenario use(BlockPos pos) { return step("use " + pos, scene -> interact(pos, false)); }
    public GameScenario sneakUseEmptyHand(BlockPos pos) {
        return equip(ItemStack.EMPTY).step("sneak use " + pos, scene -> interact(pos, true));
    }
    public GameScenario expectFluid(BlockPos pos, ResourceLocation fluid, int amount) {
        return step("expect " + amount + "mB " + fluid, scene -> {
            FluidStack actual = tank(pos).getFluidInTank(0);
            helper.assertTrue(actual.getAmount() == amount && (amount == 0 || BuiltInRegistries.FLUID.getKey(actual.getFluid()).equals(fluid)),
                    "Expected " + amount + "mB " + fluid + ", got " + actual);
        });
    }
    public GameScenario expectHeld(net.minecraft.world.item.Item item, int amount) {
        return step("expect held " + BuiltInRegistries.ITEM.getKey(item), scene -> helper.assertTrue(
                actor.getMainHandItem().is(item) && actor.getMainHandItem().getCount() == amount,
                "Unexpected held item: " + actor.getMainHandItem()));
    }
    public IFluidHandler tank(BlockPos pos) {
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(tank != null, "No fluid capability at " + pos);
        return tank;
    }
    public FermentationVesselBlockEntity vessel(BlockPos pos) {
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(entity instanceof FermentationVesselBlockEntity, "No fermentation vessel at " + pos);
        return (FermentationVesselBlockEntity) entity;
    }
    private void interact(BlockPos relative, boolean sneaking) {
        BlockPos pos = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        actor.setShiftKeyDown(sneaking);
        try {
            var state = helper.getLevel().getBlockState(pos);
            var held = actor.getMainHandItem();
            ItemInteractionResult result = state.useItemOn(held, helper.getLevel(), actor, InteractionHand.MAIN_HAND, hit);
            if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                    && state.useWithoutItem(helper.getLevel(), actor, hit).consumesAction()) return;
            if ((result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION || result == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION)
                    && !held.isEmpty()) held.useOn(new UseOnContext(helper.getLevel(), actor, InteractionHand.MAIN_HAND, held, hit));
        } finally { actor.setShiftKeyDown(false); }
    }
    private static void named(String name, Runnable action) {
        try { action.run(); }
        catch (GameTestAssertException error) { throw new GameTestAssertException("[" + name + "] " + error.getMessage()); }
        catch (RuntimeException error) { throw new IllegalStateException("Scenario step failed: " + name, error); }
    }
}
