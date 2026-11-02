package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.*;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class StillAuditGameTests {
  @GameTest(template = "barrel_space")
  public static void reusableWashCarboyRetainsOverflowAndReturnsEmpty(GameTestHelper helper) {
    var still = assemble(helper);
    FluidStack wash = wash(helper, 15000);
    still.getFluidHandler(null).fill(wash, FluidAction.EXECUTE);
    FluidContainerItem container = (FluidContainerItem) ModItems.JUICE_CARBOY.get();
    ItemStack filled = container.filled(wash.copyWithAmount(4000));
    helper.assertTrue(
        still.canPlaceItem(0, filled), "A reusable wash carboy must enter the input slot");
    still.setItem(0, filled);
    tick(helper, still, 1);
    helper.assertTrue(
        still.getFluidHandler(null).getFluidInTank(0).getAmount() == 16000
            && container.contents(still.getItem(0)).getAmount() == 3000,
        "A nearly full still must transfer only available space and preserve the remainder");
    helper.assertTrue(
        !still.canTakeItemThroughFace(0, still.getItem(0), Direction.DOWN),
        "Automation must not remove a partly emptied input");
    still.getFluidHandler(null).drain(3000, FluidAction.EXECUTE);
    tick(helper, still, 1);
    helper.assertTrue(
        container.contents(still.getItem(0)).isEmpty()
            && still.canTakeItemThroughFace(0, still.getItem(0), Direction.DOWN),
        "An emptied reusable carboy must be available to the output hopper");
    helper.assertTrue(
        BatchProfile.of(still.getFluidHandler(null).getFluidInTank(0))
            .equals(BatchProfile.of(wash)),
        "Reusable transfers must preserve the complete wash profile");
    helper.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void distillationRejectsImpossibleOutputsAndUsesRecipeContainer(
      GameTestHelper helper) {
    FluidStack wash = wash(helper, 3000);
    var input = ProductionInputs.id("agave_wash");
    var output = ProductionInputs.id("tequila");
    for (var recipe :
        List.of(
            new DistillationRecipe(
                input, output, ProductionInputs.id("fifth_bottle_empty"), 20, .4, 1000),
            new DistillationRecipe(
                input,
                output,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("stick"),
                20,
                .4,
                750),
            new DistillationRecipe(
                input,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("lava"),
                ProductionInputs.id("fifth_bottle_empty"),
                20,
                .4,
                750))) {
      helper.assertTrue(
          !recipe.matches(wash) && recipe.output(wash).isEmpty(),
          "Oversized, non-container and rejected-fluid outputs must fail before material is"
              + " consumed");
    }
    var shotRecipe =
        new DistillationRecipe(input, output, ProductionInputs.id("shot_glass"), 20, .4, 50);
    helper.assertTrue(
        shotRecipe.acceptsContainer(new ItemStack(ModItems.SHOT_GLASS.get()))
            && !shotRecipe.acceptsContainer(new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get())),
        "Each recipe must require its declared empty container, not a hardcoded fifth bottle");
    var still = assemble(helper);
    still.getFluidHandler(null).fill(wash.copyWithAmount(200), FluidAction.EXECUTE);
    // Running recipes are deliberately saved snapshots, including recipes removed by a later
    // reload.
    var saved = still.saveWithoutMetadata(helper.getLevel().registryAccess());
    saved.put(
        "DistillationPlan",
        DistillationRecipe.CODEC.codec().encodeStart(NbtOps.INSTANCE, shotRecipe).getOrThrow());
    still.loadWithComponents(saved, helper.getLevel().registryAccess());
    still.setItem(1, new ItemStack(ModItems.SHOT_GLASS.get()));
    still.setItem(2, new ItemStack(Items.COAL));
    tick(helper, still, 20);
    helper.assertTrue(
        still.getItem(3).is(ModItems.SHOT_GLASS.get())
            && ((FluidContainerItem) still.getItem(3).getItem())
                    .contents(still.getItem(3))
                    .getAmount()
                == 50
            && still.getFluidHandler(null).getFluidInTank(0).isEmpty(),
        "A configured shot recipe must consume exactly 200 mB of 10% wash for 50 mB at 40%");
    helper.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void changedContainerCancelsStalePlanAndRestartsMatchingRecipe(GameTestHelper helper) {
    var still = assemble(helper);
    still.getFluidHandler(null).fill(wash(helper, 3000), FluidAction.EXECUTE);
    var shotRecipe = new DistillationRecipe(ProductionInputs.id("agave_wash"), ProductionInputs.id("tequila"),
        ProductionInputs.id("shot_glass"), 20, .4, 50);
    var saved = still.saveWithoutMetadata(helper.getLevel().registryAccess());
    saved.put("DistillationPlan", DistillationRecipe.CODEC.codec().encodeStart(NbtOps.INSTANCE, shotRecipe).getOrThrow());
    still.loadWithComponents(saved, helper.getLevel().registryAccess());
    still.setItem(1, new ItemStack(ModItems.SHOT_GLASS.get()));
    still.setItem(2, new ItemStack(Items.COAL));
    tick(helper, still, 10);
    helper.assertTrue(still.progressTime == 10, "The saved compatible recipe must keep its progress");
    still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
    tick(helper, still, 1);
    helper.assertTrue(still.progressTime == 1 && still.processingTicks() == 1200,
        "Changing containers must discard stale shot work and select the matching fifth recipe");
    tick(helper, still, 1199);
    helper.assertTrue(still.getItem(3).is(ModItems.FIFTH_BOTTLE_EMPTY.get())
        && still.getFluidHandler(null).getFluidInTank(0).isEmpty(),
        "The replacement recipe must finish without getting stuck or consuming wash twice");
    helper.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void removedPartnerPausesBeforeAnotherHeatedTick(GameTestHelper helper) {
    var still = assemble(helper);
    still.getFluidHandler(null).fill(wash(helper, 3000), FluidAction.EXECUTE);
    still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
    still.setItem(2, new ItemStack(Items.COAL));
    tick(helper, still, 1);
    int progress = still.progressTime, heat = still.fuelTime;
    // Clear the master's block without sending neighbor updates, retaining this instance as a stale
    // ticker target.
    helper
        .getLevel()
        .setBlock(
            still.getBlockPos(),
            Blocks.AIR.defaultBlockState(),
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    tick(helper, still, 1);
    helper.assertTrue(
        still.progressTime == progress && still.fuelTime == heat,
        "An incomplete structure must never consume another unit of heat or progress");
    helper.succeed();
  }

  private static FluidStack wash(GameTestHelper helper, int amount) {
    var mosto = ProductionInputs.fromItem(new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()));
    var recipe =
        helper
            .getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
            .stream()
            .map(holder -> holder.value())
            .filter(value -> value.matches(mosto))
            .findFirst()
            .orElseThrow();
    return FermentationEngine.finish(mosto.copyWithAmount(amount), recipe);
  }

  private static StillMasterTileEntity assemble(GameTestHelper helper) {
    BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
    Block[] components = {
      ModBlocks.STEEL_BRAZIER.get(),
      ModBlocks.STEEL_POT.get(),
      ModBlocks.STEEL_COIL.get(),
      ModBlocks.STEEL_POT.get()
    };
    for (int part = 1; part <= 4; part++)
      helper
          .getLevel()
          .setBlockAndUpdate(
              StillMultiblock.position(pos, Direction.NORTH, part),
              components[part - 1].defaultBlockState());
    StillMultiblock.build(helper.getLevel(), pos);
    return (StillMasterTileEntity) helper.getLevel().getBlockEntity(pos);
  }

  private static void tick(GameTestHelper helper, StillMasterTileEntity still, int count) {
    for (int i = 0; i < count; i++)
      StillMasterTileEntity.serverTick(
          helper.getLevel(), still.getBlockPos(), still.getBlockState(), still);
  }
}
