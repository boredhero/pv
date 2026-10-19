package io.vinum.test;

import io.vinum.barrel.BarrelBottling;
import io.vinum.barrel.ModDataComponents;
import io.vinum.block.GrapeCropBlock;
import io.vinum.block.ModBlocks;
import io.vinum.crop.GrapeVariety;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.*;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** End-to-end adapters: actual crafting, container exchange, still heat and survival crop loot. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class ProductionChainGameTests {
    @GameTest(template = "barrel_space")
    public static void actualAgaveChainRequiresWashAlcoholFuelAndPreservesHistory(GameTestHelper helper) {
        FluidStack mosto = ProductionInputs.fromItem(new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()));
        FermentationRecipe fermentation = helper.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
                .stream().map(holder -> holder.value()).filter(recipe -> recipe.matches(mosto)).findFirst().orElseThrow();
        FluidStack wash = FermentationEngine.finish(mosto.copyWithAmount(3500), fermentation);
        var still = assemble(helper);
        helper.assertTrue(still.getFluidHandler(null).fill(wash, IFluidHandler.FluidAction.SIMULATE) == 3500
                && still.getFluidHandler(null).getFluidInTank(0).isEmpty(), "Simulating wash input must not mutate the machine");
        still.getFluidHandler(null).fill(wash, IFluidHandler.FluidAction.EXECUTE);
        still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
        tick(helper, still, 5);
        helper.assertTrue(still.progressTime == 0 && still.getItem(3).isEmpty(), "Wash cannot distill without heat");
        still.setItem(2, new ItemStack(Items.COAL));
        tick(helper, still, 1199);
        helper.assertTrue(still.getItem(3).isEmpty(), "New wash recipe must run its full configured duration");
        CompoundTag saved = still.saveWithoutMetadata(helper.getLevel().registryAccess());
        still.loadWithComponents(saved, helper.getLevel().registryAccess());
        tick(helper, still, 1);
        ItemStack output = still.getItem(3);
        helper.assertTrue(output.is(ModItems.FIFTH_SILVER_TEQUILA.get()), "Agave wash must distill into blanco tequila");
        helper.assertTrue(still.getFluidHandler(null).getFluidInTank(0).getAmount() == 500, "Exactly3000mB wash produces750mB40percent tequila");
        FluidStack bottled = output.get(ModDataComponents.CONTAINED_FLUID.get()).copy();
        var profile = BatchProfile.of(bottled);
        helper.assertTrue(Math.abs(profile.alcoholFraction() - .4) < 1e-9
                && profile.history().containsAll(List.of(ProductionInputs.id("fermentation"), ProductionInputs.id("distillation"))),
                "Distillation must preserve ingredient and process provenance");
        helper.assertTrue(!bottled.has(ModDataComponents.FLUID_AGE.get()), "A fresh distillate cannot inherit maturation time");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void productionContainersExchangeExactContentsAndRejectPartialFill(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tank = new FluidTank(2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAPE_JUICE_CHARDONNAY.get(), 2));
        var result = ProductionContainers.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        helper.assertTrue(result == ItemInteractionResult.CONSUME && tank.getFluidAmount() == 1000
                && player.getMainHandItem().getCount() == 1, "One prepared juice item pours exactly one litre");
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "Pouring returns exactly one glass bottle");
        tank.fill(tank.getFluid().copyWithAmount(500), IFluidHandler.FluidAction.EXECUTE);
        ProductionContainers.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        helper.assertTrue(tank.getFluidAmount() == 1500 && player.getMainHandItem().getCount() == 1, "An insufficient gap must not consume a container or partially fill");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        ProductionContainers.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        ItemStack bucket = player.getMainHandItem();
        helper.assertTrue(bucket.is(ModItems.BATCH_BUCKET.get()) && tank.getFluidAmount() == 500, "Generic bucket must withdraw exactly one litre");
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) bucket.save(helper.getLevel().registryAccess()));
        helper.assertTrue(ProductionContainers.read(restored).orElseThrow().getAmount() == 1000
                && BatchProfile.of(ProductionContainers.read(restored).orElseThrow()).sugarGramsPerLitre() == 200,
                "Item save/reload must retain both fluid amount and per-litre composition");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void stillBucketIntakeAndDismantleReturnAllWashOnce(GameTestHelper helper) {
        var still = assemble(helper);
        FluidStack mosto = ProductionInputs.fromItem(new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()));
        var recipe = helper.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(candidate -> candidate.matches(mosto)).findFirst().orElseThrow();
        FluidStack wash = FermentationEngine.finish(mosto, recipe);
        still.setItem(0, ProductionContainers.create(wash));
        tick(helper, still, 1);
        helper.assertTrue(still.getItem(0).is(Items.BUCKET) && still.getFluidHandler(null).getFluidInTank(0).getAmount() == 1000,
                "Still intake must return the empty bucket into the input slot");
        still.getFluidHandler(null).fill(wash.copyWithAmount(250), IFluidHandler.FluidAction.EXECUTE);
        helper.getLevel().destroyBlock(still.getBlockPos().above(), true);
        int total = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(still.getBlockPos()).inflate(4)).stream()
                .mapToInt(entity -> ProductionContainers.read(entity.getItem()).map(FluidStack::getAmount).orElse(0)).sum();
        helper.assertTrue(total == 1250, "Breaking any part must preserve all wash, including partial buckets, exactly once");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void allGrapesHaveMatureSurvivalLootAndReusablePreparation(GameTestHelper helper) {
        BlockPos ground = helper.absolutePos(new BlockPos(2, 0, 2));
        for (GrapeVariety variety : GrapeVariety.values()) {
            var block = (GrapeCropBlock) BuiltInRegistries.BLOCK.get(ProductionInputs.id("crop_grape_" + variety.getSerializedName()));
            helper.getLevel().setBlock(ground, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            var mature = block.defaultBlockState().setValue(GrapeCropBlock.AGE, 3);
            helper.getLevel().setBlock(ground.above(), mature, Block.UPDATE_CLIENTS);
            var drops = Block.getDrops(mature, helper.getLevel(), ground.above(), null);
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(variety.seedItem()))
                    && drops.stream().anyMatch(stack -> stack.is(variety.grapeItem())), "Mature " + variety + " must yield plantable seeds and fruit");
            CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(variety.grapeItem()), new ItemStack(ModItems.WINE_PRESS.get()), new ItemStack(Items.GLASS_BOTTLE)));
            var crafting = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
            helper.assertTrue(!ProductionInputs.fromItem(crafting.assemble(input, helper.getLevel().registryAccess())).isEmpty(), "Each grape must produce fermentable juice");
            helper.assertTrue(crafting.getRemainingItems(input).stream().anyMatch(stack -> stack.is(ModItems.WINE_PRESS.get())), "Preparing juice must preserve the press");
        }
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void realEmptyHandInteractionStartsFermentationAndAllowsAdjacentVessels(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModBlocks.CARBOY.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        var vessel = (io.vinum.production.vessel.FermentationVesselBlockEntity) helper.getLevel().getBlockEntity(pos);
        vessel.getFluidHandler(null).fill(ProductionInputs.fromItem(new ItemStack(ModItems.GRAPE_JUICE_MERLOT.get())), IFluidHandler.FluidAction.EXECUTE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        var state = helper.getLevel().getBlockState(pos);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.EAST, pos, false);
        helper.assertTrue(state.useItemOn(ItemStack.EMPTY, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, "Empty hand must reach the actual start interaction");
        helper.assertTrue(state.useWithoutItem(helper.getLevel(), player, hit).consumesAction() && vessel.isFermenting(), "Sneak-empty-hand must start fermentation through block dispatch");
        helper.assertTrue(state.useItemOn(new ItemStack(ModItems.CARBOY.get()), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION, "A held vessel must allow adjacent item placement");
        helper.succeed();
    }

    private static StillMasterTileEntity assemble(GameTestHelper helper) {
        BlockPos master = helper.absolutePos(new BlockPos(5, 1, 5));
        BlockPos output = master.relative(Direction.WEST);
        BlockPos[] positions = {master, master.above(), output.above(), output};
        Block[] components = {ModBlocks.STEEL_BRAZIER.get(), ModBlocks.STEEL_POT.get(), ModBlocks.STEEL_COIL.get(), ModBlocks.STEEL_POT.get()};
        for (int i = 0; i < positions.length; i++) helper.getLevel().setBlock(positions[i], components[i].defaultBlockState(), Block.UPDATE_CLIENTS);
        StillMultiblock.build(helper.getLevel(), positions[3]);
        return (StillMasterTileEntity) helper.getLevel().getBlockEntity(master);
    }
    private static void tick(GameTestHelper helper, StillMasterTileEntity still, int count) {
        for (int i = 0; i < count; i++) StillMasterTileEntity.serverTick(helper.getLevel(), still.getBlockPos(), still.getBlockState(), still);
    }
}
