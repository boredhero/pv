package io.vinum.test;

import io.vinum.barrel.BarrelBottling;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.BeverageCatalog;
import io.vinum.beverage.BottleCorks;
import io.vinum.beverage.FluidBlending;
import io.vinum.block.ModBlocks;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.*;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.straining.StrainerBlock;
import io.vinum.tileentity.BarrelBlockEntity;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Rum's player-facing routes share chemistry, real vessel ticks, and exact-volume transactions. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class RumGameTests {
    @GameTest(template = "empty")
    public static void reusableRumBottleCraftingRecipeLoadsAndProducesValidStack(GameTestHelper h) {
        var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, new ItemStack(Items.GLASS), ItemStack.EMPTY,
                new ItemStack(Items.GLASS), ItemStack.EMPTY, new ItemStack(Items.GLASS),
                new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS)));
        var recipe = h.getLevel().getRecipeManager().byKey(ProductionInputs.id("rum_bottle")).orElseThrow();
        h.assertTrue(recipe.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe,
                "Rum bottle has a loaded crafting recipe");
        var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) recipe.value();
        h.assertTrue(crafting.matches(input, h.getLevel()), "Glass pattern crafts the rum bottle");
        var bottle = crafting.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(bottle.is(ModItems.RUM_BOTTLE.get()) && bottle.getCount() == 1
                && bottle.getCount() <= bottle.getMaxStackSize(), "Reusable bottle recipe yields a legal single-item stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void caneCraftingShortcutRemovedButHoneySugarRemains(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(1, 1, List.of(new ItemStack(Items.SUGAR_CANE))), h.getLevel()).isEmpty(),
                "Sugar cane cannot bypass pressing and crystallization through vanilla crafting");
        var honey = recipes.getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(1, 1, List.of(new ItemStack(Items.HONEY_BOTTLE))), h.getLevel()).orElseThrow();
        h.assertTrue(honey.value().getResultItem(h.getLevel().registryAccess()).is(Items.SUGAR),
                "Honey remains a separate source of ordinary sugar");
        h.succeed();
    }

    @GameTest(template = "barrel_space", timeoutTicks = 60)
    public static void freshJuiceFermentsInBothVesselsAndRequiresCarboyAirlock(GameTestHelper h) {
        var carboy = vessel(h, new BlockPos(2, 1, 2), ModBlocks.CARBOY.get());
        var vat = vessel(h, new BlockPos(5, 1, 2), ModBlocks.FERMENTATION_VAT.get());
        FluidStack juice = fluid("cane_juice", 3000);
        var initial = BatchProfile.of(juice);
        for (var vessel : List.of(carboy, vat)) {
            h.assertTrue(vessel.getFluidHandler(null).fill(juice, FluidAction.SIMULATE) == 3000
                    && vessel.getFluid().isEmpty(), "Simulated cane input never changes the vessel");
            h.assertTrue(vessel.getFluidHandler(null).fill(juice, FluidAction.EXECUTE) == 3000,
                    "Both vessel types accept fresh cane juice");
        }
        h.assertTrue(!carboy.startFermentation(), "Open carboys cannot ferment cane juice");
        carboy.installAirlock();
        for (var vessel : List.of(carboy, vat)) {
            h.assertTrue(vessel.startFermentation(), "Both fermentation routes start without special rum machinery");
            var saved = vessel.saveWithoutMetadata(h.getLevel().registryAccess());
            saved.putInt("FermentationProgress", 23980);
            vessel.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(vessel.getFluidHandler(null).drain(1, FluidAction.EXECUTE).isEmpty(),
                    "Active rum batches cannot be extracted");
        }
        h.runAfterDelay(21, () -> {
            for (var vessel : List.of(carboy, vat)) {
                var wash = vessel.getFluid();
                h.assertTrue(!vessel.isFermenting() && wash.getAmount() == 3000
                        && BuiltInRegistries.FLUID.getKey(wash.getFluid()).equals(ProductionInputs.id("fermented_cane_wash")),
                        "Real vessel ticks finish the persisted cane fermentation plan");
                var profile = BatchProfile.of(wash);
                h.assertTrue(Math.abs(profile.alcoholFraction() - initial.sugarGramsPerLitre() * .0006) < 1e-9
                        && profile.ingredientProportions().equals(initial.ingredientProportions()),
                        "Both fermentation routes preserve cane provenance and use the same sugar conversion");
                h.assertTrue(vessel.strain() && BatchProfile.of(vessel.getFluid()).solidsGramsPerLitre() == 0,
                        "Handheld straining clears fermented cane sediment in either vessel");
            }
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void mixedFeedstocksDiluteWithoutDuplicatingSugarAndRejectUnrelatedWine(GameTestHelper h) {
        var syrup = fluid("cane_syrup", 500);
        var molasses = fluid("molasses", 500);
        double sugar = sugar(syrup) + sugar(molasses);
        var mixed = FluidBlending.blend(syrup, molasses);
        var wash = FluidBlending.blend(mixed, new FluidStack(Fluids.WATER, 3000));
        h.assertTrue(wash.getAmount() == 4000 && Math.abs(sugar(wash) - sugar) < 1e-8,
                "Mixed feedstock preparation conserves total sugar while diluting concentration");
        h.assertTrue(BuiltInRegistries.FLUID.getKey(wash.getFluid()).equals(ProductionInputs.id("cane_wash"))
                && BatchProfile.of(wash).ingredientProportions().keySet().containsAll(BatchProfile.of(syrup).ingredientProportions().keySet())
                && BatchProfile.of(wash).ingredientProportions().keySet().containsAll(BatchProfile.of(molasses).ingredientProportions().keySet()),
                "Mixed wash retains both feedstock identities");
        h.assertTrue(!FluidBlending.canBlend(wash, fluid("agave_mosto", 1000)),
                "Rum preparation cannot launder unrelated ingredients into cane wash");
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(candidate -> candidate.matches(wash)).findFirst();
        h.assertTrue(recipe.isPresent(), "Diluted mixed wash has an actual fermentation recipe");
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .noneMatch(holder -> holder.value().matches(syrup)), "Concentrated syrup cannot ferment without preparation");
        var container = (FluidContainerItem) ModItems.JUICE_CARBOY.get();
        var stored = container.filled(wash);
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(),
                (net.minecraft.nbt.CompoundTag) stored.save(h.getLevel().registryAccess()));
        h.assertTrue(FluidStack.isSameFluidSameComponents(container.contents(restored), wash)
                && container.contents(restored).getAmount() == 4000, "Mixed wash survives portable-container serialization");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void strainedWashDistillsToBulkRumThenDilutesWithoutChangingAlcohol(GameTestHelper h) {
        var juice = fluid("cane_juice", 4000);
        var fermentation = h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(recipe -> recipe.matches(juice)).findFirst().orElseThrow();
        var dirtyWash = fermentation.output(juice);
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .noneMatch(holder -> holder.value().matches(dirtyWash)), "Still rejects suspended cane sediment");
        var wash = StrainerBlock.strain(h.getLevel(), dirtyWash);
        h.assertTrue(wash.getAmount() == dirtyWash.getAmount()
                && Math.abs(alcohol(wash) - alcohol(dirtyWash)) < 1e-8, "Straining retains all fermented liquid and alcohol");
        var recipe = h.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(candidate -> candidate.matches(wash)).findFirst().orElseThrow();
        var still = assemble(h);
        still.getFluidHandler(null).fill(wash, FluidAction.EXECUTE);
        still.setItem(1, new ItemStack(ModItems.JUICE_CARBOY.get()));
        tick(h, still, 10);
        h.assertTrue(still.getItem(3).isEmpty() && still.progressTime == 0, "Rum still requires fuel");
        still.setItem(2, new ItemStack(Items.COAL));
        tick(h, still, recipe.durationTicks());
        var output = still.getItem(3);
        h.assertTrue(output.getItem() instanceof FluidContainerItem, "Rum distillation returns reusable bulk container");
        var spirit = ((FluidContainerItem) output.getItem()).contents(output);
        h.assertTrue(BuiltInRegistries.FLUID.getKey(spirit.getFluid()).equals(ProductionInputs.id("rum"))
                && spirit.getAmount() == recipe.outputMillibuckets(), "Still produces configured bulk rum output");
        double consumedAlcohol = alcohol(wash) - alcohol(still.getFluidHandler(null).getFluidInTank(0));
        h.assertTrue(Math.abs(consumedAlcohol - alcohol(spirit)) < .2,
                "Distillation conserves alcohol within the one-mB input rounding bound");
        var proofed = FluidBlending.blend(spirit, new FluidStack(Fluids.WATER, spirit.getAmount() / 2));
        h.assertTrue(Math.abs(BatchProfile.of(proofed).alcoholFraction() - .4) < 1e-8
                && Math.abs(alcohol(proofed) - alcohol(spirit)) < 1e-8,
                "Sixty-percent rum plus half its volume of water yields forty percent with no alcohol gain");
        h.succeed();
    }

    @GameTest(template = "barrel_space", timeoutTicks = 40)
    public static void rumBarrelAgesButCarboyDoesNotAndBottlingRequiresCork(GameTestHelper h) {
        var raw = fluid("rum", 750);
        long threshold = io.vinum.config.BarrelConfig.daysToTicks(io.vinum.config.BarrelConfig.rumLightAgedDays());
        var aged = BarrelFluidData.withAge(raw, threshold - 2);
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.BARREL.get().defaultBlockState());
        var barrel = (BarrelBlockEntity) h.getLevel().getBlockEntity(pos);
        barrel.getFluidHandler(null).fill(aged, FluidAction.EXECUTE);
        var carboy = vessel(h, new BlockPos(5, 1, 2), ModBlocks.CARBOY.get());
        carboy.installAirlock();
        carboy.getFluidHandler(null).fill(aged, FluidAction.EXECUTE);
        h.runAfterDelay(21, () -> {
            var matured = barrel.getFluidHandler(null).getFluidInTank(0);
            h.assertTrue(BarrelFluidData.getAge(matured) >= threshold
                    && BarrelFluidData.getAge(carboy.getFluid()) == threshold - 2,
                    "Oak aging advances in barrel; glass resting never invents oak maturity");
            h.assertTrue(BeverageCatalog.lookupFluid(matured.getFluid()).orElseThrow().aging()
                    .stage(BarrelFluidData.getAge(matured)).equals("tooltip.vinum.maturation.rum_light_aged"),
                    "Rum crosses its defined aged stage");
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            var bottleItem = BuiltInRegistries.ITEM.get(ProductionInputs.id("rum_bottle"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem));
            BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, barrel.getFluidHandler(null));
            h.assertTrue(barrel.getFluidHandler(null).getFluidInTank(0).getAmount() == 750,
                    "Missing cork cannot silently drain rum");
            player.getInventory().add(new ItemStack(ModItems.WINE_CORK.get()));
            BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, barrel.getFluidHandler(null));
            var bottle = player.getMainHandItem();
            h.assertTrue(BottleCorks.hasCork(bottle) && barrel.getFluidHandler(null).getFluidInTank(0).isEmpty()
                    && ((FluidContainerItem) bottle.getItem()).contents(bottle).getAmount() == 750,
                    "One cork seals exactly one full reusable rum bottle");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rumBottleTopUpConsumesNoSecondCork(GameTestHelper h) {
        var tank = new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1000);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var bottleItem = BuiltInRegistries.ITEM.get(ProductionInputs.id("rum_bottle"));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(bottleItem));
        player.getInventory().add(new ItemStack(ModItems.WINE_CORK.get()));
        tank.fill(fluid("rum", 500), FluidAction.EXECUTE);
        BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        h.assertTrue(BottleCorks.hasCork(player.getMainHandItem())
                && player.getInventory().countItem(ModItems.WINE_CORK.get()) == 0,
                "First rum filling consumes one cork even for a partial bottle");
        tank.fill(fluid("rum", 250), FluidAction.EXECUTE);
        BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        h.assertTrue(tank.isEmpty() && ((FluidContainerItem) bottleItem).contents(player.getMainHandItem()).getAmount() == 750,
                "Topping up an already sealed rum bottle needs no second cork");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void survivalBottleMeasuresWaterForThreeRumBottles(GameTestHelper h) {
        var source = vessel(h, new BlockPos(2, 1, 2), ModBlocks.CARBOY.get());
        var receiver = vessel(h, new BlockPos(5, 1, 2), ModBlocks.CARBOY.get());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        source.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player,
                InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(source.getBlockPos()), Direction.UP, source.getBlockPos(), false));
        h.assertTrue(source.getFluid().getAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET),
                "A survival water bucket fills a source carboy and returns the empty bucket");
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUM_BOTTLE.get()));
        BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player,
                InteractionHand.MAIN_HAND, source.getFluidHandler(null));
        var measuring = (FluidContainerItem) player.getMainHandItem().getItem();
        h.assertTrue(measuring.contents(player.getMainHandItem()).getAmount() == 750
                && source.getFluid().getAmount() == 250 && !BottleCorks.hasCork(player.getMainHandItem()),
                "An empty bottle measures exactly750mB water without requiring a cork");
        receiver.getFluidHandler(null).fill(fluid("rum", 1500), FluidAction.EXECUTE);
        player.setShiftKeyDown(true);
        BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player,
                InteractionHand.MAIN_HAND, receiver.getFluidHandler(null));
        var proofed = receiver.getFluid();
        h.assertTrue(proofed.getAmount() == 2250 && Math.abs(BatchProfile.of(proofed).alcoholFraction() - .4) < 1e-9
                && Math.abs(alcohol(proofed) - 900) < 1e-8 && measuring.contents(player.getMainHandItem()).isEmpty(),
                "A measured water pour turns two60percent still outputs into2250mB40percent rum without alcohol gain");
        player.setShiftKeyDown(false);
        player.getInventory().add(new ItemStack(ModItems.WINE_CORK.get(), 3));
        for (int i = 0; i < 3; i++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUM_BOTTLE.get()));
            BarrelBottling.useItemOn(player.getMainHandItem(), h.getLevel(), player,
                    InteractionHand.MAIN_HAND, receiver.getFluidHandler(null));
            h.assertTrue(((FluidContainerItem) player.getMainHandItem().getItem()).contents(player.getMainHandItem()).getAmount() == 750
                    && BottleCorks.hasCork(player.getMainHandItem()), "Each measured batch yields a sealed750mB bottle");
        }
        h.assertTrue(receiver.getFluid().isEmpty() && player.getInventory().countItem(ModItems.WINE_CORK.get()) == 0,
                "Exactly three bottles and three corks consume the proofed batch");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rumMaturityStagesChangeAtBothConfiguredThresholds(GameTestHelper h) {
        var sample = fluid("rum", 750);
        var aging = BeverageCatalog.lookupFluid(sample.getFluid()).orElseThrow().aging();
        long light = io.vinum.config.BarrelConfig.daysToTicks(io.vinum.config.BarrelConfig.rumLightAgedDays());
        long dark = io.vinum.config.BarrelConfig.daysToTicks(io.vinum.config.BarrelConfig.rumDarkAgedDays());
        h.assertTrue(aging.stage(light - 1).equals("tooltip.vinum.maturation.rum_white")
                && aging.stage(light).equals("tooltip.vinum.maturation.rum_light_aged")
                && aging.stage(dark - 1).equals("tooltip.vinum.maturation.rum_light_aged")
                && aging.stage(dark).equals("tooltip.vinum.maturation.rum_dark_aged"),
                "Rum transitions from white to light-aged to dark-aged at exact configured thresholds");
        h.succeed();
    }

    private static FluidStack fluid(String id, int amount) {
        var fluid = new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(id)), amount);
        return ProductionInputs.defaultProfile(fluid).applyTo(fluid);
    }
    private static double sugar(FluidStack fluid) { return BatchProfile.of(fluid).sugarGramsPerLitre() * fluid.getAmount() / 1000; }
    private static double alcohol(FluidStack fluid) { return BatchProfile.of(fluid).alcoholFraction() * fluid.getAmount(); }
    private static FermentationVesselBlockEntity vessel(GameTestHelper h, BlockPos relative, Block block) {
        var pos = h.absolutePos(relative);
        h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        return (FermentationVesselBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static StillMasterTileEntity assemble(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(4, 1, 4));
        Block[] components = {ModBlocks.STEEL_BRAZIER.get(), ModBlocks.STEEL_POT.get(), ModBlocks.STEEL_COIL.get(), ModBlocks.STEEL_POT.get()};
        for (int part = 1; part <= 4; part++) h.getLevel().setBlockAndUpdate(
                StillMultiblock.position(pos, Direction.NORTH, part), components[part - 1].defaultBlockState());
        StillMultiblock.build(h.getLevel(), pos);
        return (StillMasterTileEntity) h.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper h, StillMasterTileEntity still, int count) {
        for (int i = 0; i < count; i++) StillMasterTileEntity.serverTick(h.getLevel(), still.getBlockPos(), still.getBlockState(), still);
    }
}
