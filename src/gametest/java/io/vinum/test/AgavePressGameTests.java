package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.machine.WinePressBlockEntity;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionContainers;
import io.vinum.production.ProductionInputs;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import java.util.Map;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual extraction, container transfer, fermentation and distillation share one agave batch. */
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class AgavePressGameTests {
  @GameTest(template = "barrel_space", timeoutTicks = 60)
  public static void cookedAgavePressFeedsFermentationAndTequilaDistillation(
      GameTestHelper helper) {
    var level = helper.getLevel();
    BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
    level.setBlockAndUpdate(pos, ModBlocks.WINE_PRESS.get().defaultBlockState());
    ModBlocks.WINE_PRESS
        .get()
        .setPlacedBy(
            level, pos, level.getBlockState(pos), null, new ItemStack(ModItems.WINE_PRESS.get()));
    var press = (WinePressBlockEntity) level.getBlockEntity(pos);
    var top = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.UP);
    helper.assertTrue(top != null, "A hopper above the press can supply ingredients");
    helper.assertTrue(
        !top.insertItem(0, new ItemStack(ModItems.AGAVE.get()), false).isEmpty(),
        "Raw agave must be cooked before extraction");
    helper.assertTrue(
        top.insertItem(0, new ItemStack(ModItems.COOKED_AGAVE_PULP.get(), 3), false).isEmpty(),
        "The shared press accepts cooked agave through top automation");
    pressTicks(helper, press, 200);
    helper.assertTrue(
        press.fluids().getFluidInTank(0).isEmpty()
            && press.inventory.getStackInSlot(0).getCount() == 3,
        "Cooked agave cannot extract liquid without mechanical work");
    press.attachMotor();
    press.energy.generate(6000);
    pressTicks(helper, press, 600);
    var mosto = press.fluids().getFluidInTank(0);
    var profile = BatchProfile.of(mosto);
    helper.assertTrue(
        mosto.getAmount() == 3000
            && BuiltInRegistries.FLUID
                .getKey(mosto.getFluid())
                .equals(ProductionInputs.id("agave_mosto"))
            && press.inventory.getStackInSlot(0).isEmpty()
            && press.energy.getEnergyStored() == 0,
        "Three cooked pulp and 6000 FE yield exactly three litres of fermentable agave liquid");
    helper.assertTrue(
        Math.abs(profile.sugarGramsPerLitre() - 500D / 3) < 1e-9
            && profile.alcoholFraction() == 0
            && profile.ingredientProportions().equals(Map.of(ProductionInputs.id("agave"), 1D)),
        "Extracted agave retains its sugar and identity without inventing alcohol");
    var shortcut =
        CraftingInput.of(
            2,
            1,
            List.of(
                new ItemStack(ModItems.COOKED_AGAVE_PULP.get()),
                new ItemStack(Items.WATER_BUCKET)));
    helper.assertTrue(
        level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, shortcut, level).isEmpty(),
        "Water-and-pulp crafting must not bypass the press");

    press.inventory.setStackInSlot(1, new ItemStack(ModItems.JUICE_CARBOY.get()));
    pressTicks(helper, press, 1);
    ItemStack filled = press.inventory.extractItem(2, 1, false);
    helper.assertTrue(
        filled.getItem() instanceof FluidContainerItem
            && ((FluidContainerItem) filled.getItem()).contents(filled).getAmount() == 3000
            && press.fluids().getFluidInTank(0).isEmpty(),
        "The press fills a reusable carboy with the whole extracted batch");
    BlockPos vesselPos = helper.absolutePos(new BlockPos(2, 1, 5));
    level.setBlockAndUpdate(vesselPos, ModBlocks.CARBOY.get().defaultBlockState());
    var vessel = (FermentationVesselBlockEntity) level.getBlockEntity(vesselPos);
    var player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setShiftKeyDown(true);
    player.setItemInHand(InteractionHand.MAIN_HAND, filled);
    ProductionContainers.useItemOn(
        filled, level, player, InteractionHand.MAIN_HAND, vessel.getFluidHandler(null));
    helper.assertTrue(
        vessel.getFluid().getAmount() == 3000
            && ((FluidContainerItem) player.getMainHandItem().getItem())
                .contents(player.getMainHandItem())
                .isEmpty()
            && vessel.startFermentation(),
        "Pressed mosto pours and starts actual vessel fermentation");
    var saved = vessel.saveWithoutMetadata(level.registryAccess());
    saved.putInt("FermentationProgress", 23980);
    vessel.loadWithComponents(saved, level.registryAccess());
    helper.runAfterDelay(
        21,
        () -> {
          var wash = vessel.getFluid();
          helper.assertTrue(
              wash.getAmount() == 3000
                  && BuiltInRegistries.FLUID
                      .getKey(wash.getFluid())
                      .equals(ProductionInputs.id("agave_wash"))
                  && Math.abs(BatchProfile.of(wash).alcoholFraction() - .1) < 1e-9,
              "The vessel converts the pressed three-litre batch into ten-percent wash");
          player.setShiftKeyDown(false);
          ProductionContainers.useItemOn(
              player.getMainHandItem(),
              level,
              player,
              InteractionHand.MAIN_HAND,
              vessel.getFluidHandler(null));
          var still = assemble(helper);
          still.setItem(0, player.getMainHandItem().copy());
          still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
          still.setItem(2, new ItemStack(Items.COAL));
          for (int tick = 0; tick < 1200; tick++)
            StillMasterTileEntity.serverTick(
                level, still.getBlockPos(), still.getBlockState(), still);
          ItemStack bottle = still.getItem(3);
          helper.assertTrue(
              bottle.is(ModItems.FIFTH_BOTTLE_EMPTY.get()),
              "The chain yields a reusable tequila fifth");
          var tequila = ((FluidContainerItem) bottle.getItem()).contents(bottle);
          helper.assertTrue(
              tequila.getAmount() == 750
                  && Math.abs(BatchProfile.of(tequila).alcoholFraction() - .4) < 1e-9
                  && BatchProfile.of(tequila)
                      .ingredientProportions()
                      .equals(profile.ingredientProportions())
                  && BatchProfile.of(tequila)
                      .history()
                      .containsAll(
                          List.of(
                              ProductionInputs.id("fermentation"),
                              ProductionInputs.id("distillation")))
                  && still.getFluidHandler(null).getFluidInTank(0).isEmpty(),
              "Actual distillation preserves agave provenance and conserves 300 mB of alcohol");
          helper.assertTrue(
              vessel.getFluid().isEmpty()
                  && ((FluidContainerItem) still.getItem(0).getItem())
                      .contents(still.getItem(0))
                      .isEmpty(),
              "Transfers leave no duplicated liquid and return the reusable carboy");
          helper.succeed();
        });
  }

  private static void pressTicks(GameTestHelper helper, WinePressBlockEntity press, int count) {
    for (int tick = 0; tick < count; tick++)
      WinePressBlockEntity.tick(
          helper.getLevel(), press.getBlockPos(), press.getBlockState(), press);
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
}
