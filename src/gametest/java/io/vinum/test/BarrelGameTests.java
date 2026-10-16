package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.block.WineBarrelBlock;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.BarrelBottling;
import io.vinum.barrel.BarrelLegacyMigration;
import io.vinum.barrel.ModDataComponents;
import io.vinum.fluid.ModFluids;
import io.vinum.item.ModItems;
import io.vinum.multiblock.BarrelMultiblock;
import io.vinum.multiblock.BarrelShapes;
import io.vinum.tileentity.BarrelBlockEntity;
import io.vinum.tileentity.ModTileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests the universal barrel using real world entities and the same assembly service as placement. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BarrelGameTests {
    private static final BlockPos ORIGIN = new BlockPos(2, 1, 2);

    @GameTest(template = "barrel_space")
    public static void barrelsExpandThroughEveryCubeSizeOneToNine(GameTestHelper helper) {
        for (int size = 1; size <= 9; size++) {
            placeCube(helper, size, Direction.NORTH);
            BarrelMultiblock.reconcile(helper.getLevel(), helper.absolutePos(ORIGIN.offset(size - 1, size - 1, size - 1)));
            assertStructure(helper, size);
        }
        helper.assertTrue(barrel(helper, helper.absolutePos(ORIGIN)).getCapacity() == 164_025_000,
                "Maximum cube must expose exactly 729 times the per-barrel capacity without integer overflow");
        IFluidHandler maximum = tank(helper, helper.absolutePos(ORIGIN));
        long transferStarted = System.nanoTime();
        helper.assertTrue(maximum.fill(new FluidStack(Fluids.WATER, 164_025_000), IFluidHandler.FluidAction.EXECUTE) == 164_025_000,
                "Maximum cube must accept its full capacity across729 local reservoirs");
        for (int query = 0; query < 100; query++) {
            helper.assertTrue(maximum.drain(1_000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1_000, "Repeated simulated automation queries must remain stable");
        }
        helper.assertTrue(maximum.drain(1_000_000, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1_000_000
                && maximum.getFluidInTank(0).getAmount() == 163_025_000, "Large transfer must conserve exact aggregate volume");
        org.slf4j.LoggerFactory.getLogger(BarrelGameTests.class).info("Maximum9-cube: full fill,100 simulated drains,and1M mB drain completed in {} ms",
                (System.nanoTime() - transferStarted) / 1_000_000.0);
        helper.succeed();
    }

    @GameTest(template = "barrel_space", timeoutTicks = 400)
    public static void placedBarrelsAutomaticallyExpandFromTwoToThree(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        helper.startSequence()
                .thenWaitUntil(() -> assertStructure(helper, 2))
                .thenExecute(() -> placeCube(helper, 3, Direction.NORTH))
                .thenWaitUntil(() -> assertStructure(helper, 3))
                .thenSucceed();
    }

    @GameTest(template = "barrel_space")
    public static void oversizedCubeNeverCreatesStructureAboveNineOrLosesBlocks(GameTestHelper helper) {
        placeCube(helper, 10, Direction.NORTH);
        long started = System.nanoTime();
        BarrelMultiblock.reconcile(helper.getLevel(), helper.absolutePos(ORIGIN));
        long elapsed = System.nanoTime() - started;
        org.slf4j.LoggerFactory.getLogger(BarrelGameTests.class).info("Oversized10-cube bounded reconciliation completed in {} ms", elapsed / 1_000_000.0);
        int count = 0;
        for (BlockPos pos : cube(helper, 10)) {
            BarrelBlockEntity barrel = barrel(helper, pos);
            helper.assertTrue(barrel.getStructureSize() >= 1 && barrel.getStructureSize() <= 9, "Oversized component must not form an illegal reservoir");
            count++;
        }
        helper.assertTrue(count == 1000, "All 1000 placed barrels must remain present");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void incompleteOrDifferentlyFacingCubeDoesNotMerge(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BlockPos corner = helper.absolutePos(ORIGIN.offset(1, 1, 1));
        helper.getLevel().setBlock(corner, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BarrelMultiblock.reconcile(helper.getLevel(), helper.absolutePos(ORIGIN));
        helper.assertTrue(barrel(helper, helper.absolutePos(ORIGIN)).getStructureSize() == 1, "Incomplete cube must remain separate");
        helper.getLevel().setBlock(corner, ModBlocks.BARREL.get().defaultBlockState().setValue(WineBarrelBlock.FACING, Direction.EAST), Block.UPDATE_CLIENTS);
        BarrelMultiblock.reconcile(helper.getLevel(), helper.absolutePos(ORIGIN));
        helper.assertTrue(barrel(helper, helper.absolutePos(ORIGIN)).getStructureSize() == 1, "Different barrel facing must prevent merge");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void barrelCapacitySimulationAndFluidCompatibilityAreConservative(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        helper.assertTrue(tank.getTanks() == 1 && tank.getTankCapacity(0) == 225_000, "Single barrel default capacity must be 225000 mB");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE) == 1_000, "Barrel must accept water");
        CompoundTag beforeSimulation = barrel(helper, helper.absolutePos(ORIGIN)).saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 400), IFluidHandler.FluidAction.SIMULATE) == 400, "Simulated fill reports available capacity");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1_000, "Simulated fill must not mutate contents");
        helper.assertTrue(tank.drain(300, IFluidHandler.FluidAction.SIMULATE).getAmount() == 300, "Simulated drain reports removable fluid");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1_000, "Simulated drain must not mutate contents");
        helper.assertTrue(beforeSimulation.equals(barrel(helper, helper.absolutePos(ORIGIN)).saveWithoutMetadata(helper.getLevel().registryAccess())),
                "Simulation must not change persisted fluid, age reference, or structure metadata");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE) == 0, "Mixed fluids must be rejected");
        helper.assertTrue(tank.drain(new FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Drain of wrong fluid must not remove contents");
        helper.assertTrue(tank.drain(300, IFluidHandler.FluidAction.EXECUTE).getAmount() == 300, "Executed drain removes requested quantity");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1_000_000), IFluidHandler.FluidAction.EXECUTE) == 224_300, "Fill must stop at exact remaining capacity");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 225_000, "Tank must not overflow or lose existing fluid");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void cubeCapacityAndFillAreSharedAcrossAllMembers(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BarrelMultiblock.reconcile(helper.getLevel(), helper.absolutePos(ORIGIN));
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        helper.assertTrue(tank.getTankCapacity(0) == 1_800_000, "Two-cube capacity must include all eight members");
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1_000_000), IFluidHandler.FluidAction.EXECUTE) == 1_000_000, "Fill must span multiple physical barrels");
        int localTotal = 0;
        for (BlockPos pos : cube(helper, 2)) {
            helper.assertTrue(tank(helper, pos).getFluidInTank(0).getAmount() == 1_000_000, "All faces must report same aggregate fluid");
            localTotal += barrel(helper, pos).getLocalFluid().getAmount();
        }
        helper.assertTrue(localTotal == 1_000_000, "Local escrow sum must equal aggregate volume");
        IFluidHandler opposite = tank(helper, helper.absolutePos(ORIGIN.offset(1, 1, 1)));
        helper.assertTrue(opposite.drain(123_456, IFluidHandler.FluidAction.EXECUTE).getAmount() == 123_456, "Any member must drain shared reservoir");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 876_544, "All members must observe drain exactly once");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void barrelsContainingDifferentFluidsCannotMerge(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BlockPos origin = helper.absolutePos(ORIGIN);
        BlockPos opposite = origin.offset(1, 1, 1);
        tank(helper, origin).fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        tank(helper, opposite).fill(new FluidStack(Fluids.LAVA, 2_000), IFluidHandler.FluidAction.EXECUTE);
        BarrelMultiblock.reconcile(helper.getLevel(), origin);
        for (BlockPos pos : cube(helper, 2)) helper.assertTrue(barrel(helper, pos).getStructureSize() == 1, "Conflicting fluids must leave cube unmerged");
        helper.assertTrue(tank(helper, origin).getFluidInTank(0).getAmount() == 1_000 && tank(helper, opposite).getFluidInTank(0).getAmount() == 2_000,
                "Rejected merge must preserve both original fluid quantities");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void breakingAndReplacingGroupedBarrelConservesAllFluid(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BlockPos origin = helper.absolutePos(ORIGIN);
        BarrelMultiblock.reconcile(helper.getLevel(), origin);
        tank(helper, origin).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 1_000_000), 800), IFluidHandler.FluidAction.EXECUTE);
        int removedVolume = barrel(helper, origin).getLocalFluid().getAmount();
        helper.getLevel().destroyBlock(origin, true);
        int retained = 0;
        for (BlockPos pos : cube(helper, 2)) {
            if (pos.equals(origin)) continue;
            BarrelBlockEntity member = barrel(helper, pos);
            helper.assertTrue(member.getStructureSize() == 1, "Broken cube must dissolve into individual barrels");
            helper.assertTrue(member.getLocalAge() == 800, "Splitting must preserve each member's fluid age");
            retained += member.getLocalFluid().getAmount();
        }
        helper.assertTrue(retained + removedVolume == 1_000_000, "Split must conserve each member's local fluid escrow");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Broken barrel must produce exactly one item");
        ItemStack dropped = drops.getFirst().getItem().copy();
        drops.getFirst().discard();
        helper.assertTrue(dropped.getItem() instanceof BlockItem, "Harvested barrel must remain placeable");
        helper.assertTrue(dropped.getMaxStackSize() == 1, "Filled barrel items must not stack and multiply their stored fluid");
        BlockPos replacement = helper.absolutePos(new BlockPos(12, 1, 12));
        helper.getLevel().setBlockAndUpdate(replacement.below(), Blocks.STONE.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, dropped);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(replacement), Direction.UP, replacement.below(), false);
        var result = ((BlockItem) dropped.getItem()).place(new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, dropped, hit));
        helper.assertTrue(result.consumesAction(), "Harvested filled barrel must be placeable");
        BarrelBlockEntity restored = barrel(helper, replacement);
        helper.assertTrue(restored.getLocalFluid().getAmount() == removedVolume, "Replacement must restore only dropped member's own fluid");
        helper.assertTrue(restored.getLocalAge() == 800, "Harvested barrel must preserve the age of its own contents");
        helper.assertTrue(restored.getStructureSize() == 1 && restored.getStructureOrigin().equals(replacement), "Dropped item must not retain former group ownership");
        helper.assertTrue(retained + restored.getLocalFluid().getAmount() == 1_000_000, "Break and replacement must not duplicate or lose fluid");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Successful placement must consume the one harvested barrel item");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void differentAgesMergeByVolumeAndDrainWithoutChangingRemainingAge(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BlockPos origin = helper.absolutePos(ORIGIN);
        BlockPos opposite = origin.offset(1, 1, 1);
        tank(helper, origin).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 1_000), 100), IFluidHandler.FluidAction.EXECUTE);
        tank(helper, opposite).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 3_000), 300), IFluidHandler.FluidAction.EXECUTE);
        BarrelMultiblock.reconcile(helper.getLevel(), origin);
        assertStructure(helper, 2);
        IFluidHandler merged = tank(helper, origin);
        helper.assertTrue(merged.getFluidInTank(0).getAmount() == 4_000 && BarrelFluidData.getAge(merged.getFluidInTank(0)) == 250,
                "Merge must weight age by fluid quantity, not barrel count");
        merged.fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 4_000), 750), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(BarrelFluidData.getAge(merged.getFluidInTank(0)) == 500, "Adding older fluid must preserve weighted age");
        FluidStack removed = merged.drain(2_000, IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(removed.getAmount() == 2_000 && BarrelFluidData.getAge(removed) == 500, "Drained portion must carry its weighted age");
        helper.assertTrue(merged.getFluidInTank(0).getAmount() == 6_000 && BarrelFluidData.getAge(merged.getFluidInTank(0)) == 500,
                "Partial drain must preserve remaining volume and age");
        long ageMass = 0;
        for (BlockPos pos : cube(helper, 2)) {
            FluidStack local = barrel(helper, pos).getLocalFluid();
            ageMass += (long) local.getAmount() * BarrelFluidData.getAge(local);
        }
        helper.assertTrue(ageMass == 3_000_000, "Local age-volume sum must match shared reservoir after mixing");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void immediateSplitAfterMergeCannotRecoverUnmixedOlderFluid(GameTestHelper helper) {
        placeCube(helper, 2, Direction.NORTH);
        BlockPos origin = helper.absolutePos(ORIGIN);
        BlockPos opposite = origin.offset(1, 1, 1);
        long oldAge = 365L * 24_000;
        long blendedAge = oldAge / 4;
        tank(helper, origin).fill(BarrelFluidData.withAge(new FluidStack(ModFluids.TEQUILA.source().get(), 1_000), oldAge), IFluidHandler.FluidAction.EXECUTE);
        tank(helper, opposite).fill(new FluidStack(ModFluids.TEQUILA.source().get(), 3_000), IFluidHandler.FluidAction.EXECUTE);
        BarrelMultiblock.reconcile(helper.getLevel(), origin);
        assertStructure(helper, 2);
        // No fill or drain after formation: merging itself must homogenize the batch.
        helper.assertTrue(barrel(helper, origin).getLocalFluid().getAmount() == 1_000
                && barrel(helper, opposite).getLocalFluid().getAmount() == 3_000, "Merge must preserve physical ownership of each fluid amount");
        helper.assertTrue(barrel(helper, origin).getLocalAge() == blendedAge && barrel(helper, opposite).getLocalAge() == blendedAge,
                "Formation must blend each nonempty local escrow immediately, not only the aggregate display");
        helper.getLevel().destroyBlock(origin, true);
        int retained = 0;
        for (BlockPos pos : cube(helper, 2)) {
            if (pos.equals(origin)) continue;
            BarrelBlockEntity member = barrel(helper, pos);
            FluidStack local = member.getLocalFluid();
            retained += local.getAmount();
            if (!local.isEmpty()) helper.assertTrue(member.getLocalAge() == blendedAge, "Surviving portion must retain blended age after immediate split");
        }
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(2));
        helper.assertTrue(drops.size() == 1, "Immediate split must produce one harvested barrel");
        ItemStack dropped = drops.getFirst().getItem();
        var data = dropped.get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(data != null, "Harvested filled barrel must carry its local fluid snapshot");
        BarrelBlockEntity restored = new BarrelBlockEntity(origin, ModBlocks.BARREL.get().defaultBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(data.copyTag(), helper.getLevel().registryAccess());
        helper.assertTrue(restored.getLocalFluid().getAmount() == 1_000 && restored.getLocalAge() == blendedAge,
                "Broken barrel item must contain its blended portion, never recover the original365-day-old batch");
        helper.assertTrue(retained == 3_000 && retained + restored.getLocalFluid().getAmount() == 4_000,
                "Age homogenization and immediate teardown must preserve total fluid exactly");
        helper.succeed();
    }

    @GameTest(template = "barrel_space", timeoutTicks = 30)
    public static void agingUsesElapsedWorldTimePersistsAndResetsWhenEmpty(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity original = barrel(helper, helper.absolutePos(ORIGIN));
        IFluidHandler tank = tank(helper, original.getBlockPos());
        tank.fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 1_000), 90), IFluidHandler.FluidAction.EXECUTE);
        long start = helper.getLevel().getGameTime();
        helper.runAfterDelay(6, () -> {
            long age = 90 + helper.getLevel().getGameTime() - start;
            helper.assertTrue(original.getLocalAge() == age, "Barrel aging must track elapsed world ticks without active per-barrel ticking");
            CompoundTag saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            BarrelBlockEntity restored = new BarrelBlockEntity(original.getBlockPos(), original.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getLocalFluid().getAmount() == 1_000 && restored.getLocalAge() == age, "Save/load must preserve fluid and elapsed age");
            tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(original.getLocalFluid().isEmpty() && original.getLocalAge() == 0, "Draining empty must clear aging history");
            tank.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(original.getLocalAge() == 0, "Fresh refill must not inherit previous batch age");
            helper.succeed();
        });
    }

    @GameTest(template = "barrel_space")
    public static void vanillaBucketsTransferWholeVolumesAndRejectPartialCapacity(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BlockPos pos = helper.absolutePos(ORIGIN);
        IFluidHandler tank = tank(helper, pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, helper.getLevel(), pos, Direction.UP), "Water bucket must empty into barrel");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank.getFluidInTank(0).getAmount() == 1_000, "Bucket emptying must conserve exactly 1000 mB and return empty bucket");
        helper.assertTrue(FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, helper.getLevel(), pos, Direction.UP), "Empty bucket must collect barrel water");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && tank.getFluidInTank(0).isEmpty(), "Bucket filling must remove exactly 1000 mB");
        tank.fill(new FluidStack(Fluids.WATER, 224_500), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(!FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, helper.getLevel(), pos, Direction.UP), "Half-bucket free space must reject whole bucket transfer");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET) && tank.getFluidInTank(0).getAmount() == 224_500, "Rejected transfer must preserve held bucket and barrel fluid");
        tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        tank.fill(new FluidStack(Fluids.WATER, 999), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertTrue(!FluidUtil.interactWithFluidHandler(player, InteractionHand.MAIN_HAND, helper.getLevel(), pos, Direction.UP), "Insufficient fluid must not partly fill bucket");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank.getFluidInTank(0).getAmount() == 999, "Failed extraction must preserve both container and fluid");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void unavailableChunkRejectsTransfersWithoutLoadingOrLosingEscrow(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity original = barrel(helper, helper.absolutePos(ORIGIN));
        tank(helper, original.getBlockPos()).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 4_000), 250), IFluidHandler.FluidAction.EXECUTE);
        CompoundTag saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockPos unavailable = original.getBlockPos().offset(1_000_000, 0, 1_000_000);
        helper.assertTrue(!helper.getLevel().hasChunkAt(unavailable), "Guard fixture must target an unloaded chunk");
        // A restored entity may exist before its owning structure's chunks become available.
        BarrelBlockEntity restored = new BarrelBlockEntity(unavailable, original.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        int loaded = helper.getLevel().getChunkSource().getLoadedChunksCount();
        helper.assertTrue(!restored.isStructureLoaded(), "Absent chunks must make reservoir unavailable");
        IFluidHandler guarded = restored.getFluidHandler(Direction.UP);
        helper.assertTrue(guarded.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE) == 0, "Unavailable structures must reject fill");
        helper.assertTrue(guarded.drain(1_000, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "Unavailable structures must reject drain");
        helper.assertTrue(restored.getLocalFluid().getAmount() == 4_000 && restored.getLocalAge() == 250, "Guarded access must preserve saved local escrow");
        helper.assertTrue(!helper.getLevel().hasChunkAt(unavailable) && helper.getLevel().getChunkSource().getLoadedChunksCount() == loaded,
                "Capability inspection must not force-load absent chunks");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void tequilaBottlingUsesExactAgeBoundariesAndPreservesFluidAge(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        long day = 24_000L;
        long[] ages = {0, 60 * day - 1, 60 * day, 365 * day - 1, 365 * day, 1095 * day - 1, 1095 * day};
        Item[] expected = {ModItems.FIFTH_SILVER_TEQUILA.get(), ModItems.FIFTH_SILVER_TEQUILA.get(), ModItems.FIFTH_REPOSADO_TEQUILA.get(),
                ModItems.FIFTH_REPOSADO_TEQUILA.get(), ModItems.FIFTH_ANEJO_TEQUILA.get(), ModItems.FIFTH_ANEJO_TEQUILA.get(), ModItems.FIFTH_EXTRA_ANEJO_TEQUILA.get()};
        for (int index = 0; index < ages.length; index++) {
            tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            tank.fill(BarrelFluidData.withAge(new FluidStack(ModFluids.TEQUILA.source().get(), 750), ages[index]), IFluidHandler.FluidAction.EXECUTE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
            bottle(helper, player, tank);
            helper.assertTrue(player.getMainHandItem().is(expected[index]) && player.getMainHandItem().getCount() == 1,
                    "Tequila age " + ages[index] + " must bottle as " + expected[index]);
            helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "Bottling must remove exactly 750 mB");
            helper.assertTrue(player.getMainHandItem().getOrDefault(ModDataComponents.FLUID_AGE.get(), 0L) == ages[index], "Bottled item must retain exact age snapshot");
            bottle(helper, player, tank);
            helper.assertTrue(player.getMainHandItem().is(ModItems.FIFTH_BOTTLE_EMPTY.get()), "Pouring bottle back must return empty glass");
            helper.assertTrue(tank.getFluidInTank(0).getAmount() == 750 && BarrelFluidData.getAge(tank.getFluidInTank(0)) == ages[index],
                    "Bottle roundtrip must preserve volume and exact age");
        }
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void wineBottlesAndGlassesWithdrawTheirOwnServingVolumes(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tank.fill(new FluidStack(ModFluids.CHARDONNAY.source().get(), 1_000), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get()));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(ModItems.WINE_BOTTLE_CHARDONNAY.get()) && tank.getFluidInTank(0).getAmount() == 250,
                "Chardonnay bottle must withdraw 750 mB and retain wine identity");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WINE_GLASS_EMPTY.get()));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(ModItems.WINE_GLASS_CHARDONNAY.get()) && tank.getFluidInTank(0).getAmount() == 125,
                "Wine glass must withdraw 125 mB and retain wine identity");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void partialBottleTransfersNeverConsumeContainerOrFluid(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tank.fill(new FluidStack(ModFluids.TEQUILA.source().get(), 749), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(ModItems.FIFTH_BOTTLE_EMPTY.get()) && player.getMainHandItem().getCount() == 1
                && tank.getFluidInTank(0).getAmount() == 749, "Short serving must preserve empty bottle and all remaining fluid");
        tank.fill(new FluidStack(ModFluids.TEQUILA.source().get(), 224_000), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get()));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(ModItems.FIFTH_SILVER_TEQUILA.get()) && tank.getFluidInTank(0).getAmount() == 224_749,
                "Insufficient capacity must preserve filled bottle and existing fluid");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void stackedBottleInteractionReturnsExactlyOneFilledContainer(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tank.fill(new FluidStack(ModFluids.TEQUILA.source().get(), 750), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get(), 2));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(ModItems.FIFTH_BOTTLE_EMPTY.get()) && player.getMainHandItem().getCount() == 1,
                "Bottling from a stack must consume only one empty container");
        helper.assertTrue(player.getInventory().countItem(ModItems.FIFTH_SILVER_TEQUILA.get()) == 1 && tank.getFluidInTank(0).isEmpty(),
                "Bottling must deliver one filled container without fluid duplication");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void agedVanillaBucketRoundtripKeepsFluidAge(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        tank.fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 1_000), 1_000), IFluidHandler.FluidAction.EXECUTE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET)
                && player.getMainHandItem().getOrDefault(ModDataComponents.FLUID_AGE.get(), 0L) == 1_000
                && tank.getFluidInTank(0).isEmpty(), "Barrel bucket interaction must preserve age on extracted water");
        bottle(helper, player, tank);
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && tank.getFluidInTank(0).getAmount() == 1_000
                && BarrelFluidData.getAge(tank.getFluidInTank(0)) == 1_000, "Pouring aged bucket back must preserve volume and age");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void filledBarrelCraftingRemainderPreservesOneCopyOfContents(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity barrel = barrel(helper, helper.absolutePos(ORIGIN));
        tank(helper, barrel.getBlockPos()).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 4_000), 1_234), IFluidHandler.FluidAction.EXECUTE);
        ItemStack storedBarrel = new ItemStack(ModItems.BARREL.get());
        barrel.saveToItem(storedBarrel, helper.getLevel().registryAccess());
        CraftingInput input = CraftingInput.of(2, 1, java.util.List.of(new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()), storedBarrel));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
        var result = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModItems.FERMENTED_AGAVE_WORT.get()) && result.getCount() == 1, "Filled barrel must remain usable in legacy fermentation recipe");
        var remainders = recipe.getRemainingItems(input);
        int barrels = remainders.stream().filter(stack -> stack.is(ModItems.BARREL.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(barrels == 1, "Crafting must return exactly one filled barrel, without duplicating stored contents");
        ItemStack remainder = remainders.stream().filter(stack -> stack.is(ModItems.BARREL.get())).findFirst().orElseThrow();
        helper.assertTrue(ItemStack.isSameItemSameComponents(storedBarrel, remainder), "Craft remainder must retain fluid, age and item metadata exactly");
        remainder.shrink(1);
        helper.assertTrue(storedBarrel.getCount() == 1, "Craft remainder must be a separate stack, not alias the ingredient");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void barrelInspectionAndTransferInputsCannotMutateStoredState(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity barrel = barrel(helper, helper.absolutePos(ORIGIN));
        IFluidHandler tank = tank(helper, barrel.getBlockPos());
        FluidStack supplied = BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 4_000), 250);
        tank.fill(supplied, IFluidHandler.FluidAction.EXECUTE);
        supplied.setAmount(1);
        var snapshot = barrel.getContents();
        helper.assertTrue(snapshot.available() && snapshot.amount() == 4_000 && snapshot.capacity() == 225_000
                && snapshot.fluidType() == Fluids.WATER.getFluidType(), "Public snapshot must expose correct availability, volume, capacity and fluid type");
        FluidStack exposed = snapshot.fluid();
        exposed.setAmount(2);
        exposed.set(ModDataComponents.FLUID_AGE.get(), 999L);
        barrel.getLocalFluid().setAmount(3);
        barrel.getFluid().setAmount(4);
        tank.getFluidInTank(0).setAmount(5);
        helper.assertTrue(snapshot.amount() == 4_000 && snapshot.fluidContentAge() == 250,
                "Snapshot fluid accessor must return a defensive copy");
        helper.assertTrue(barrel.getFluid().getAmount() == 4_000 && barrel.getLocalAge() == 250,
                "Mutating public fluid views and original fill input must not alter persisted contents");
        helper.succeed();
    }

    @GameTest(template = "barrel_space", timeoutTicks = 30)
    public static void extremeAgeMixingAndElapsedTimeSaturateWithoutOverflow(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity barrel = barrel(helper, helper.absolutePos(ORIGIN));
        IFluidHandler tank = tank(helper, barrel.getBlockPos());
        tank.fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 2_000), Long.MAX_VALUE), IFluidHandler.FluidAction.EXECUTE);
        tank.fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 2_000), Long.MAX_VALUE - 2), IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(barrel.getLocalAge() == Long.MAX_VALUE - 1 && barrel.getFluid().getAmount() == 4_000,
                "Weighted age multiplication must not overflow for very old externally supplied fluid");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(barrel.getLocalAge() == Long.MAX_VALUE && barrel.getContents().fluidContentAge() == Long.MAX_VALUE,
                    "Elapsed aging must saturate at maximum long, never wrap to negative or fresh");
            CompoundTag saved = barrel.saveWithFullMetadata(helper.getLevel().registryAccess());
            BarrelBlockEntity restored = new BarrelBlockEntity(barrel.getBlockPos(), barrel.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            helper.assertTrue(restored.getLocalAge() == Long.MAX_VALUE && restored.getLocalFluid().getAmount() == 4_000,
                    "Extreme age and volume must survive serialization");
            helper.succeed();
        });
    }

    @GameTest(template = "barrel_space")
    public static void emptyHarvestedBarrelsStackAndKeepUnrelatedItemMetadata(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BarrelBlockEntity barrel = barrel(helper, helper.absolutePos(ORIGIN));
        IFluidHandler tank = tank(helper, barrel.getBlockPos());
        tank.fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        ItemStack stack = new ItemStack(ModItems.BARREL.get());
        Component name = Component.literal("Cellar barrel");
        ItemLore lore = new ItemLore(java.util.List.of(Component.literal("A remembered harvest")));
        stack.set(DataComponents.CUSTOM_NAME, name);
        stack.set(DataComponents.LORE, lore);
        barrel.saveToItem(stack, helper.getLevel().registryAccess());
        helper.assertTrue(stack.has(DataComponents.BLOCK_ENTITY_DATA) && stack.getMaxStackSize() == 1, "Filled barrel must retain unstackable reservoir data");
        tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        barrel.saveToItem(stack, helper.getLevel().registryAccess());
        helper.assertTrue(!stack.has(DataComponents.BLOCK_ENTITY_DATA) && stack.getMaxStackSize() == 64,
                "Empty barrel serialization must remove stale reservoir data and permit normal stacking");
        helper.assertTrue(name.equals(stack.get(DataComponents.CUSTOM_NAME)) && lore.equals(stack.get(DataComponents.LORE)),
                "Clearing empty storage must preserve unrelated custom name and lore");
        var drops = Block.getDrops(barrel.getBlockState(), helper.getLevel(), barrel.getBlockPos(), barrel);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.BARREL.get()) && drops.getFirst().getMaxStackSize() == 64,
                "Normally harvested empty barrels must stack with newly crafted barrels");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void emptyHandShowsStatusWhileHeldBarrelStillPlacesAdjacent(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        BlockPos pos = helper.absolutePos(ORIGIN);
        var state = helper.getLevel().getBlockState(pos);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.5, 0, 0), Direction.EAST, pos, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(state.useItemOn(ItemStack.EMPTY, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "Empty hand must reach the barrel's status interaction");
        helper.assertTrue(state.useWithoutItem(helper.getLevel(), player, hit).consumesAction(), "Default empty-hand interaction must handle barrel status");
        ItemStack held = new ItemStack(ModItems.BARREL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        helper.assertTrue(state.useItemOn(held, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, "Held building block must bypass status so item placement can proceed");
        helper.assertTrue(held.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, held, hit)).consumesAction(),
                "Held barrel must actually place next to the clicked barrel");
        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).is(ModBlocks.BARREL.get()) && player.getMainHandItem().isEmpty(),
                "Adjacent placement must create one barrel and consume the held item");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void legacyDecorativeBarrelsGainEntitiesWithoutChangingExistingStorage(GameTestHelper helper) {
        BlockPos legacyPos = helper.absolutePos(ORIGIN);
        BlockPos storedPos = legacyPos.above();
        helper.getLevel().setBlockAndUpdate(legacyPos, ModBlocks.BLOCK_WINE_BARREL_CHARDONNAY.get().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(storedPos, ModBlocks.BARREL.get().defaultBlockState());
        BarrelBlockEntity existing = barrel(helper, storedPos);
        tank(helper, storedPos).fill(BarrelFluidData.withAge(new FluidStack(Fluids.WATER, 4_000), 250), IFluidHandler.FluidAction.EXECUTE);
        var chunk = helper.getLevel().getChunkAt(legacyPos);
        chunk.removeBlockEntity(legacyPos);
        helper.assertTrue(!chunk.getBlockEntities().containsKey(legacyPos)
                && chunk.getBlockState(legacyPos).is(ModBlocks.BLOCK_WINE_BARREL_CHARDONNAY.get()),
                "Migration fixture must retain legacy barrel state with no saved block entity");
        int repaired = BarrelLegacyMigration.repair(chunk);
        // Inspect the chunk map directly: Level.getBlockEntity would lazily hide a missing migration.
        helper.assertTrue(repaired == 1 && chunk.getBlockEntities().get(legacyPos) instanceof BarrelBlockEntity,
                "Legacy repair must eagerly create the missing entity needed by barrel rendering");
        BarrelBlockEntity migrated = (BarrelBlockEntity) chunk.getBlockEntities().get(legacyPos);
        helper.assertTrue(migrated.getType() == ModTileEntities.BARREL.get() && migrated.getLocalFluid().isEmpty(),
                "Legacy decorative wine label must migrate to valid empty storage without inventing wine");
        helper.assertTrue(chunk.getBlockEntities().get(storedPos) == existing && existing.getLocalFluid().getAmount() == 4_000
                && existing.getLocalAge() == 250, "Migration must preserve existing entity identity, contents and age");
        helper.assertTrue(BarrelLegacyMigration.repair(chunk) == 0 && chunk.getBlockEntities().get(storedPos) == existing
                && chunk.getBlockEntities().get(legacyPos) == migrated, "Repeated migration must be idempotent and leave existing entities intact");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void raisedBarrelCradlesSupportBodyAndKeepClearanceInEveryOrientation(GameTestHelper helper) {
        for (int size : new int[]{1, 2, 9}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                helper.assertTrue(occupies(size, facing, 0.2, 0.05, 0.22), "Front cradle must touch ground beneath raised body at size " + size + " facing " + facing);
                helper.assertTrue(occupies(size, facing, 0.2, 0.05, 0.78), "Rear cradle must provide the second solid support");
                helper.assertTrue(!occupies(size, facing, 0.5, 0.02, 0.5), "Raised barrel must leave clearance between its two stands");
                helper.assertTrue(!occupies(size, facing, 0.05, 0.02, 0.22), "Cradle collision must not extend outside its footprint");
                helper.assertTrue(occupies(size, facing, 0.5, 0.55, 0.5), "Barrel body center must remain solid");
                for (BlockPos part : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(size - 1, size - 1, size - 1))) {
                    for (AABB box : BarrelShapes.shape(part, BlockPos.ZERO, size, facing).toAabbs()) {
                        helper.assertTrue(box.minX >= 0 && box.minY >= 0 && box.minZ >= 0
                                && box.maxX <= 1 && box.maxY <= 1 && box.maxZ <= 1,
                                "Every body and support collision box must stay within its owning block at size " + size + " facing " + facing);
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void bottlesGlassesAndBucketsPreserveAllFluidMetadataAcrossItemSave(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Fluid[] fluids = {ModFluids.TEQUILA.source().get(), ModFluids.CHARDONNAY.source().get(), ModFluids.CHARDONNAY.source().get(), Fluids.WATER};
        Item[] empty = {ModItems.FIFTH_BOTTLE_EMPTY.get(), ModItems.WINE_BOTTLE_EMPTY.get(), ModItems.WINE_GLASS_EMPTY.get(), Items.BUCKET};
        Item[] filled = {ModItems.FIFTH_SILVER_TEQUILA.get(), ModItems.WINE_BOTTLE_CHARDONNAY.get(), ModItems.WINE_GLASS_CHARDONNAY.get(), Items.WATER_BUCKET};
        int[] volumes = {750, 750, 125, 1_000};
        for (int index = 0; index < fluids.length; index++) {
            tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            FluidStack original = BarrelFluidData.withAge(new FluidStack(fluids[index], volumes[index]), 250);
            CompoundTag marker = new CompoundTag();
            marker.putString("quality", "estate-reserve");
            marker.putInt("batch", 17);
            original.set(DataComponents.CUSTOM_NAME, Component.literal("Special cellar batch"));
            original.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
            tank.fill(original, IFluidHandler.FluidAction.EXECUTE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(empty[index]));
            bottle(helper, player, tank);
            helper.assertTrue(player.getMainHandItem().is(filled[index]) && tank.getFluidInTank(0).isEmpty(),
                    "Metadata-bearing fluid must fill the correct exact-volume serving");
            ItemStack reloaded = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                    (CompoundTag) player.getMainHandItem().saveOptional(helper.getLevel().registryAccess()));
            helper.assertTrue(!reloaded.isEmpty(), "Serving item and embedded fluid metadata must serialize successfully");
            player.setItemInHand(InteractionHand.MAIN_HAND, reloaded);
            bottle(helper, player, tank);
            helper.assertTrue(player.getMainHandItem().is(empty[index]) && tank.getFluidInTank(0).getAmount() == volumes[index]
                    && FluidStack.isSameFluidSameComponents(original, tank.getFluidInTank(0)),
                    "Serving roundtrip must preserve fluid identity, exact amount, age, custom name and batch data for " + filled[index]);
        }
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void malformedServingFluidPayloadIsRejectedWithoutConsumption(GameTestHelper helper) {
        placeCube(helper, 1, Direction.NORTH);
        IFluidHandler tank = tank(helper, helper.absolutePos(ORIGIN));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        FluidStack[] malformed = {new FluidStack(Fluids.WATER, 750), new FluidStack(ModFluids.TEQUILA.source().get(), 749)};
        for (FluidStack payload : malformed) {
            ItemStack held = new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get());
            held.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(payload));
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            bottle(helper, player, tank);
            helper.assertTrue(player.getMainHandItem() == held && held.getCount() == 1 && tank.getFluidInTank(0).isEmpty(),
                    "Wrong fluid identity or serving volume must reject without consuming item or inventing fluid");
            helper.assertTrue(held.get(ModDataComponents.CONTAINED_FLUID.get()).matches(payload), "Rejected serving must keep its original payload intact");
        }
        helper.succeed();
    }

    private static void placeCube(GameTestHelper helper, int size, Direction facing) {
        for (BlockPos pos : cube(helper, size)) {
            helper.getLevel().setBlock(pos, ModBlocks.BARREL.get().defaultBlockState().setValue(WineBarrelBlock.FACING, facing), Block.UPDATE_CLIENTS);
        }
    }

    private static Iterable<BlockPos> cube(GameTestHelper helper, int size) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        return BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1));
    }

    private static BarrelBlockEntity barrel(GameTestHelper helper, BlockPos absolute) {
        helper.assertTrue(helper.getLevel().getBlockEntity(absolute) instanceof BarrelBlockEntity, "Expected barrel entity at " + absolute);
        return (BarrelBlockEntity) helper.getLevel().getBlockEntity(absolute);
    }

    private static IFluidHandler tank(GameTestHelper helper, BlockPos pos) {
        IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, pos, Direction.UP);
        helper.assertTrue(tank != null, "Barrel must expose fluid capability at " + pos);
        return tank;
    }

    private static void bottle(GameTestHelper helper, Player player, IFluidHandler tank) {
        BarrelBottling.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, tank);
    }

    private static boolean occupies(int size, Direction facing, double lateral, double vertical, double axial) {
        double x = (facing.getAxis() == Direction.Axis.X ? axial : lateral) * size;
        double y = vertical * size;
        double z = (facing.getAxis() == Direction.Axis.X ? lateral : axial) * size;
        BlockPos part = BlockPos.containing(x, y, z);
        double localX = x - part.getX(), localY = y - part.getY(), localZ = z - part.getZ();
        return BarrelShapes.shape(part, BlockPos.ZERO, size, facing).toAabbs().stream()
                .anyMatch(box -> box.contains(localX, localY, localZ));
    }

    private static void assertStructure(GameTestHelper helper, int size) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        for (BlockPos pos : cube(helper, size)) {
            BarrelBlockEntity barrel = barrel(helper, pos);
            helper.assertTrue(barrel.getStructureSize() == size && barrel.getStructureOrigin().equals(origin),
                    "Every member of " + size + "³ cube must share size and origin; failed at " + pos);
        }
    }
}
