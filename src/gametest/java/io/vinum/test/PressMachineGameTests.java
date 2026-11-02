package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.*;
import io.vinum.machine.*;
import io.vinum.production.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class PressMachineGameTests {
  private static final BlockPos PRESS = new BlockPos(3, 2, 3);

  private static WinePressBlockEntity press(GameTestHelper h) {
    h.setBlock(PRESS, ModBlocks.WINE_PRESS.get());
    var state = h.getBlockState(PRESS);
    ModBlocks.WINE_PRESS
        .get()
        .setPlacedBy(
            h.getLevel(),
            h.absolutePos(PRESS),
            state,
            null,
            new ItemStack(ModItems.WINE_PRESS.get()));
    return (WinePressBlockEntity) h.getBlockEntity(PRESS);
  }

  private static void work(GameTestHelper h, WinePressBlockEntity press, int ticks) {
    for (int i = 0; i < ticks; i++)
      WinePressBlockEntity.tick(h.getLevel(), press.getBlockPos(), press.getBlockState(), press);
  }

  @GameTest(template = "barrel_space")
  public static void pressRequiresPowerAndTopAutomationAcceptsOnlyGrapes(GameTestHelper h) {
    var press = press(h);
    var input =
        h.getLevel()
            .getCapability(
                Capabilities.ItemHandler.BLOCK, press.getBlockPos().above(), Direction.UP);
    h.assertTrue(input != null, "Top item capability is exposed");
    h.assertTrue(
        !input.insertItem(0, new ItemStack(Items.STONE), false).isEmpty(),
        "Reject unrelated input");
    h.assertTrue(
        input.insertItem(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 2), true).isEmpty()
            && press.inventory.getStackInSlot(0).isEmpty(),
        "Simulated input does not mutate inventory");
    input.insertItem(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 2), false);
    h.assertTrue(input.extractItem(0, 1, false).isEmpty(), "Top automation cannot steal input");
    work(h, press, 250);
    h.assertTrue(
        press.inventory.getStackInSlot(0).getCount() == 2
            && press.fluids().getFluidInTank(0).isEmpty(),
        "No free juice without crank or FE");
    h.assertTrue(
        press.energy.receiveEnergy(80, true) == 80 && press.energy.getEnergyStored() == 0,
        "FE simulation is immutable");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void poweredPressProducesExactJuiceAndFillsCarboy(GameTestHelper h) {
    var press = press(h);
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 2));
    press.attachMotor();
    press.energy.generate(2000);
    work(h, press, 200);
    var juice = press.fluids().getFluidInTank(0);
    h.assertTrue(
        juice.getAmount() == 1000
            && press.inventory.getStackInSlot(0).getCount() == 1
            && press.energy.getEnergyStored() == 0,
        "One grape and 2000 FE produce exactly one litre");
    h.assertTrue(
        BatchProfile.of(juice).sugarGramsPerLitre() == 200
            && BatchProfile.of(juice)
                .ingredientProportions()
                .containsKey(ProductionInputs.id("grapes_merlot")),
        "Juice retains its composition");
    h.assertTrue(
        press.fluids().fill(juice, FluidAction.EXECUTE) == 0,
        "Output tank cannot accept injected fluids");
    press.inventory.setStackInSlot(1, new ItemStack(ModItems.JUICE_CARBOY.get()));
    work(h, press, 1);
    ItemStack filled = press.inventory.getStackInSlot(2);
    h.assertTrue(
        ((FluidContainerItem) filled.getItem()).contents(filled).getAmount() == 1000
            && press.fluids().getFluidInTank(0).isEmpty(),
        "Container fill transfers exact juice once");
    h.assertTrue(
        press.items(Direction.DOWN).extractItem(0, 1, false).is(ModItems.JUICE_CARBOY.get()),
        "Bottom hopper extracts the filled container");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void fullOrDifferentJuiceStopsBeforeSpendingInputAndPower(GameTestHelper h) {
    var press = press(h);
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 10));
    press.attachMotor();
    for (int batch = 0; batch < 8; batch++) {
      press.energy.generate(2000);
      work(h, press, 200);
    }
    press.attachMotor();
    press.energy.generate(2000);
    work(h, press, 20);
    h.assertTrue(
        press.fluids().getFluidInTank(0).getAmount() == 8000
            && press.inventory.getStackInSlot(0).getCount() == 2
            && press.energy.getEnergyStored() == 2000,
        "Full tank blocks without consuming power or grapes");
    press.attachCrank();
    h.assertTrue(
        !press.turnCrank()
            && press.crankTurns() == 0
            && press.data.get(1) == 0
            && press.energy.getEnergyStored() == 2000,
        "Blocked output rejects manual turns without movement, work or power consumption");
    press.fluids().drain(1000, FluidAction.EXECUTE);
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_CHARDONNAY.get()));
    work(h, press, 200);
    h.assertTrue(
        press.energy.getEnergyStored() == 2000 && press.inventory.getStackInSlot(0).getCount() == 1,
        "Different juice cannot replace existing contents");
    h.succeed();
  }

  @GameTest(template = "barrel_space", timeoutTicks = 40)
  public static void installedCrankHasServerCooldownAndMenuDistanceChecks(GameTestHelper h) {
    var press = press(h);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.moveTo(Vec3.atCenterOf(press.getBlockPos()));
    ItemStack crank = new ItemStack(ModItems.HAND_CRANK.get(), 2);
    player.setItemInHand(InteractionHand.MAIN_HAND, crank);
    var upper = press.getBlockPos().above();
    h.getLevel()
        .getBlockState(upper)
        .useItemOn(
            crank,
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(upper), Direction.UP, upper, false));
    h.assertTrue(press.hasCrank() && crank.getCount() == 1, "Attaching consumes exactly one crank");
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get()));
    h.assertTrue(
        press.turnCrank()
            && !press.turnCrank()
            && press.data.get(1) == 250
            && press.energy.getEnergyStored() == 0,
        "Repeated interaction cannot bypass cooldown");
    var menu = new WinePressMenu(1, player.getInventory(), press);
    player.moveTo(
        press.getBlockPos().getX() + 100, press.getBlockPos().getY(), press.getBlockPos().getZ());
    h.assertTrue(!menu.clickMenuButton(player, 0), "Remote menu button cannot power the press");
    h.runAfterDelay(
        10,
        () -> {
          h.assertTrue(
              press.data.get(1) == 250
                  && press.turnCrank()
                  && press.data.get(1) == 500
                  && press.energy.getEnergyStored() == 0,
              "Manual work pauses between turns and advances directly without generating FE");
          h.succeed();
        });
  }

  @GameTest(template = "barrel_space")
  public static void fuelGeneratorPowersAdjacentPressAndConservesFE(GameTestHelper h) {
    var press = press(h);
    press.attachMotor();
    var generatorPos = PRESS.east();
    h.setBlock(generatorPos, ModBlocks.BASIC_GENERATOR.get());
    var generator = (BasicGeneratorBlockEntity) h.getBlockEntity(generatorPos);
    generator.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 2));
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get()));
    for (int i = 0; i < 250; i++) {
      BasicGeneratorBlockEntity.tick(
          h.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
      work(h, press, 1);
    }
    h.assertTrue(
        press.fluids().getFluidInTank(0).getAmount() == 1000
            && generator.inventory.getStackInSlot(0).getCount() == 1,
        "Coal generator powers the actual adjacent press");
    h.assertTrue(
        generator.energy.getEnergyStored() + press.energy.getEnergyStored() + 2000
            == 250 * BasicGeneratorBlockEntity.FE_PER_TICK,
        "Transfer and processing conserve generated FE");
    h.assertTrue(generator.energy.receiveEnergy(100, false) == 0, "Generator is output-only");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void generatorPausesWhenFullAndReturnsFuelContainers(GameTestHelper h) {
    h.setBlock(PRESS, ModBlocks.BASIC_GENERATOR.get());
    var generator = (BasicGeneratorBlockEntity) h.getBlockEntity(PRESS);
    generator.energy.generate(BasicGeneratorBlockEntity.CAPACITY);
    generator.inventory.setStackInSlot(0, new ItemStack(Items.LAVA_BUCKET));
    BasicGeneratorBlockEntity.tick(
        h.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
    h.assertTrue(
        generator.inventory.getStackInSlot(0).is(Items.LAVA_BUCKET)
            && generator.burnRemaining() == 0,
        "Full generator leaves fuel untouched");
    generator.energy.consume(20);
    BasicGeneratorBlockEntity.tick(
        h.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
    h.assertTrue(
        generator.inventory.getStackInSlot(0).isEmpty()
            && generator.inventory.getStackInSlot(1).is(Items.BUCKET),
        "Lava fuel returns its bucket exactly once");
    int remaining = generator.burnRemaining();
    BasicGeneratorBlockEntity.tick(
        h.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
    h.assertTrue(
        generator.burnRemaining() == remaining,
        "Burn pauses when there is nowhere to store energy");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void harvestedPressRetainsCrankJuiceInventoryAndWork(GameTestHelper h) {
    var press = press(h);
    press.attachCrank();
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 3));
    press.attachMotor();
    press.energy.generate(2500);
    work(h, press, 225);
    var drops = Block.getDrops(press.getBlockState(), h.getLevel(), press.getBlockPos(), press);
    h.assertTrue(
        drops.size() == 1 && drops.getFirst().getMaxStackSize() == 1,
        "Machine drops once with unstackable stored state");
    var restored = new WinePressBlockEntity(press.getBlockPos(), press.getBlockState());
    restored.setLevel(h.getLevel());
    restored.loadWithComponents(
        drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),
        h.getLevel().registryAccess());
    h.assertTrue(
        restored.hasCrank()
            && restored.fluids().getFluidInTank(0).getAmount() == 1000
            && restored.inventory.getStackInSlot(0).getCount() == 2,
        "Drop keeps attached crank and materials");
    h.assertTrue(
        restored.energy.getEnergyStored() == 250 && restored.progressFraction() == .125F,
        "Drop keeps stored power and paid progress");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void motorInstallationControlsFEAndUpperBreakDropsOneCompletePress(
      GameTestHelper h) {
    var press = press(h);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var upper = press.getBlockPos().above();
    h.assertTrue(
        h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, upper, Direction.UP) == null,
        "An unupgraded press has no external power input");
    ItemStack motor = new ItemStack(ModItems.PRESS_MOTOR.get(), 2);
    player.setItemInHand(InteractionHand.MAIN_HAND, motor);
    h.getLevel()
        .getBlockState(upper)
        .useItemOn(
            motor,
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(upper), Direction.NORTH, upper, false));
    var input = h.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, upper, Direction.UP);
    h.assertTrue(
        press.hasMotor()
            && motor.getCount() == 1
            && input != null
            && input.receiveEnergy(80, false) == 80,
        "Installing motor from upper half consumes one upgrade and enables FE");
    press.attachCrank();
    h.getLevel().destroyBlock(upper, true);
    h.assertTrue(
        h.getLevel().getBlockState(press.getBlockPos()).isAir()
            && h.getLevel().getBlockState(upper).isAir(),
        "Breaking the top removes both occupied blocks");
    var drops =
        h
            .getLevel()
            .getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(press.getBlockPos()).inflate(2))
            .stream()
            .filter(entity -> entity.getItem().is(ModItems.WINE_PRESS.get()))
            .toList();
    h.assertTrue(
        drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
        "Exactly one machine is dropped");
    var restored = new WinePressBlockEntity(press.getBlockPos(), press.getBlockState());
    restored.setLevel(h.getLevel());
    restored.loadWithComponents(
        drops
            .getFirst()
            .getItem()
            .get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
            .copyTag(),
        h.getLevel().registryAccess());
    h.assertTrue(
        restored.hasMotor() && restored.hasCrank() && restored.energy.getEnergyStored() == 80,
        "Upper-half break preserves both upgrades and power");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void pressPlacementRequiresTwoClearBlocks(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var pos = h.absolutePos(PRESS);
    h.setBlock(PRESS.below(), net.minecraft.world.level.block.Blocks.STONE);
    h.setBlock(PRESS.above(), net.minecraft.world.level.block.Blocks.STONE);
    ItemStack item = new ItemStack(ModItems.WINE_PRESS.get(), 2);
    player.setItemInHand(InteractionHand.MAIN_HAND, item);
    var hit =
        new BlockHitResult(
            Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
    var context =
        new net.minecraft.world.item.context.UseOnContext(
            h.getLevel(), player, InteractionHand.MAIN_HAND, item, hit);
    h.assertTrue(
        !item.useOn(context).consumesAction()
            && item.getCount() == 2
            && h.getLevel().getBlockState(pos).isAir(),
        "Occupied headroom prevents placement without consuming a machine");
    h.getLevel().removeBlock(pos.above(), false);
    h.assertTrue(
        item.useOn(context).consumesAction()
            && item.getCount() == 1
            && h.getLevel().getBlockState(pos.above()).is(ModBlocks.WINE_PRESS.get()),
        "Normal item use places both halves and consumes exactly one machine");
    h.succeed();
  }

  @GameTest(template = "barrel_space", timeoutTicks = 100)
  public static void eightUpperHandleTurnsPressOneGrapeWithoutFEOrBackgroundWork(GameTestHelper h) {
    var press = press(h);
    press.attachCrank();
    press.inventory.setStackInSlot(0, new ItemStack(ModItems.GRAPES_MERLOT.get(), 2));
    // Even stale stored charge cannot run the machine without a motor.
    press.energy.generate(80);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.moveTo(Vec3.atCenterOf(press.getBlockPos()));
    var upper = press.getBlockPos().above();
    var hit =
        new BlockHitResult(
            new Vec3(upper.getX() + .85, upper.getY() + .23, upper.getZ() + .5),
            Direction.EAST,
            upper,
            false);
    for (int turn = 0; turn < 8; turn++) {
      final int number = turn + 1;
      h.runAfterDelay(
          1 + turn * WinePressBlockEntity.CRANK_COOLDOWN,
          () -> {
            h.assertTrue(
                press.data.get(1) == (number - 1) * WinePressBlockEntity.CRANK_WORK,
                "Manual progress remains stationary between turns");
            h.getLevel().getBlockState(upper).useWithoutItem(h.getLevel(), player, hit);
            h.assertTrue(
                press.crankTurns() == number && press.energy.getEnergyStored() == 80,
                "Physical handle interaction advances rotation without generating or spending FE");
            if (number < 8) {
              h.assertTrue(
                  press.data.get(1) == number * WinePressBlockEntity.CRANK_WORK
                      && press.fluids().getFluidInTank(0).isEmpty(),
                  "Each click moves progress immediately but cannot finish early");
            } else {
              h.assertTrue(
                  press.fluids().getFluidInTank(0).getAmount() == 1000
                      && press.inventory.getStackInSlot(0).getCount() == 1,
                  "Eight turns consume exactly one grape and create exactly one litre");
              work(h, press, 200);
              h.assertTrue(
                  press.data.get(1) == 0 && press.inventory.getStackInSlot(0).getCount() == 1,
                  "Stopping the crank cannot process the next grape");
              var menu = new WinePressMenu(1, player.getInventory(), press);
              h.assertTrue(
                  menu.hasCrank() && !menu.hasMotor(), "Manual menu has no motor/FE presentation");
              var replica = new WinePressBlockEntity(press.getBlockPos(), press.getBlockState());
              replica.setLevel(h.getLevel());
              replica.loadWithComponents(
                  press.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
              h.assertTrue(
                  replica.crankTurns() == 8 && replica.lastCrank() == press.lastCrank(),
                  "Block-entity updates carry the rotation count and animation start time");
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "barrel_space", timeoutTicks = 100)
  public static void manualClarificationRequiresEightTurnsAndPreservesBatch(GameTestHelper h) {
    var press = press(h);
    press.attachCrank();
    h.setBlock(PRESS.west(), ModBlocks.FERMENTATION_VAT.get());
    var vat =
        (io.vinum.production.vessel.FermentationVesselBlockEntity) h.getBlockEntity(PRESS.west());
    var raw = ProductionInputs.fromItem(new ItemStack(ModItems.GRAPE_MUST_MERLOT.get()));
    var recipe =
        h
            .getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
            .stream()
            .map(holder -> holder.value())
            .filter(candidate -> candidate.matches(raw))
            .findFirst()
            .orElseThrow();
    var finished = FermentationEngine.finish(raw, recipe);
    vat.getFluidHandler(null).fill(finished, FluidAction.EXECUTE);
    h.assertTrue(
        press.clarifyAdjacentVat() && press.data.get(1) == 0,
        "Selecting a neighboring finished vat does not perform free separation");
    for (int turn = 0; turn < 8; turn++) {
      final int number = turn + 1;
      h.runAfterDelay(
          1 + turn * WinePressBlockEntity.CRANK_COOLDOWN,
          () -> {
            h.assertTrue(press.turnCrank(), "A manual turn contributes to the selected vat");
            h.assertTrue(press.energy.getEnergyStored() == 0, "Manual clarification uses no FE");
            if (number < 8)
              h.assertTrue(
                  BatchProfile.of(vat.getFluid()).solidsGramsPerLitre() > 0,
                  "Skins remain until all work is complete");
            else {
              var after = vat.getFluid();
              var beforeProfile = BatchProfile.of(finished);
              var afterProfile = BatchProfile.of(after);
              h.assertTrue(
                  after.getAmount() == finished.getAmount()
                      && afterProfile.solidsGramsPerLitre() == 0
                      && afterProfile.alcoholFraction() == beforeProfile.alcoholFraction()
                      && afterProfile
                          .ingredientProportions()
                          .equals(beforeProfile.ingredientProportions()),
                  "Separation preserves liquid, alcohol and grape proportions");
              h.assertTrue(
                  !press.clarifyAdjacentVat(), "Already clear wine cannot be selected again");
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "barrel_space", timeoutTicks = 30)
  public static void changingSelectedVatCancelsManualClarificationWithoutTouchingReplacement(
      GameTestHelper h) {
    var press = press(h);
    press.attachCrank();
    h.setBlock(PRESS.west(), ModBlocks.FERMENTATION_VAT.get());
    var vat =
        (io.vinum.production.vessel.FermentationVesselBlockEntity) h.getBlockEntity(PRESS.west());
    var raw = ProductionInputs.fromItem(new ItemStack(ModItems.GRAPE_MUST_MERLOT.get()));
    var recipe =
        h
            .getLevel()
            .getRecipeManager()
            .getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get())
            .stream()
            .map(holder -> holder.value())
            .filter(candidate -> candidate.matches(raw))
            .findFirst()
            .orElseThrow();
    vat.getFluidHandler(null).fill(FermentationEngine.finish(raw, recipe), FluidAction.EXECUTE);
    h.assertTrue(
        press.clarifyAdjacentVat() && press.turnCrank(),
        "Begin manual work against a particular batch");
    vat.getFluidHandler(null).drain(1, FluidAction.EXECUTE);
    work(h, press, 1);
    h.assertTrue(
        press.requiredWork() == 0
            && press.data.get(1) == 0
            && vat.getFluid().getAmount() == 999
            && BatchProfile.of(vat.getFluid()).solidsGramsPerLitre() > 0,
        "Changing the selected batch cancels work without changing its fluid");
    h.succeed();
  }
}
