package io.vinum.test;

import io.vinum.barrel.BarrelBottling;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.FluidBlending;
import io.vinum.beverage.WineCatalog;
import io.vinum.block.ModBlocks;
import io.vinum.config.BarrelConfig;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionInputs;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.tileentity.BarrelBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class WineAuditGameTests {
  private static FluidStack wine(String variety, int amount) {
    return WineCatalog.sample(
        WineCatalog.varieties().stream()
            .filter(v -> v.legacyFluid().getPath().equals(variety))
            .findFirst()
            .orElseThrow(),
        amount);
  }

  @GameTest(template = "barrel_space")
  public static void canonicalWineAndBlendsKeepMaturationWhenBottled(GameTestHelper helper) {
    BlockPos pos = new BlockPos(2, 2, 2);
    helper.setBlock(pos, ModBlocks.BARREL.get());
    var barrel = (BarrelBlockEntity) helper.getBlockEntity(pos);
    var tank = barrel.getFluidHandler(null);
    var player = helper.makeMockPlayer(GameType.SURVIVAL);
    long[] ages = {
      0,
      BarrelConfig.daysToTicks(BarrelConfig.wineMatureDays()),
      BarrelConfig.daysToTicks(BarrelConfig.wineReserveDays())
    };
    String[] stages = {"young", "mature", "reserve"};
    for (boolean mixed : new boolean[] {false, true}) {
      for (int stage = 0; stage < ages.length; stage++) {
        FluidStack source =
            mixed
                ? FluidBlending.blend(wine("merlot", 375), wine("chardonnay", 375))
                : wine("merlot", 750);
        source = BarrelFluidData.withAge(source, ages[stage]);
        helper.assertTrue(
            tank.fill(source, FluidAction.EXECUTE) == 750, "Barrel accepts canonical wine");
        player.setItemInHand(
            InteractionHand.MAIN_HAND, new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get()));
        BarrelBottling.useItemOn(
            player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, tank);
        ItemStack bottle = player.getMainHandItem();
        var item = (FluidContainerItem) bottle.getItem();
        FluidStack contents = item.contents(bottle);
        helper.assertTrue(
            contents.getAmount() == 750 && tank.getFluidInTank(0).isEmpty(),
            "Bottling transfers exact volume");
        helper.assertTrue(
            BarrelFluidData.getAge(contents) == ages[stage]
                && BatchProfile.of(contents).equals(BatchProfile.of(source)),
            "Bottling preserves age and composition");
        List<Component> tooltip = new ArrayList<>();
        item.appendHoverText(
            bottle, Item.TooltipContext.of(helper.getLevel()), tooltip, TooltipFlag.NORMAL);
        String expected = "tooltip.vinum.maturation." + stages[stage];
        helper.assertTrue(
            tooltip.stream()
                .anyMatch(
                    line ->
                        line.getContents() instanceof TranslatableContents text
                            && text.getKey().equals("tooltip.vinum.maturation")
                            && text.getArgs()[0] instanceof Component name
                            && name.getContents() instanceof TranslatableContents maturation
                            && maturation.getKey().equals(expected)),
            "Canonical bottle exposes " + expected + " for pure wine and blends");
        helper.assertTrue(
            tooltip.stream()
                .anyMatch(
                    line ->
                        line.getContents() instanceof TranslatableContents text
                            && text.getKey().equals("tooltip.vinum.barrel_age")),
            "Canonical bottle exposes elapsed age");
      }
    }
    helper.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void mustNeedsClarificationForServingButCanTravelInBulkContainers(
      GameTestHelper helper) {
    FluidStack cloudy = wine("merlot", 1000);
    BatchProfile base = BatchProfile.of(cloudy);
    new BatchProfile(
            0,
            0,
            100,
            base.alcoholFraction(),
            base.ingredientProportions(),
            base.yeast(),
            List.of(ProductionInputs.id("fermentation")))
        .applyTo(cloudy);
    for (Item vessel : List.of(ModItems.WINE_GLASS_EMPTY.get(), ModItems.SHOT_GLASS.get())) {
      var handler = ((FluidContainerItem) vessel).handler(new ItemStack(vessel));
      helper.assertTrue(
          handler.fill(cloudy, FluidAction.EXECUTE) == 0 && handler.getFluidInTank(0).isEmpty(),
          "Serving glasses reject unclarified solids without consuming them");
    }
    var carboy =
        ((FluidContainerItem) ModItems.JUICE_CARBOY.get())
            .handler(new ItemStack(ModItems.JUICE_CARBOY.get()));
    helper.assertTrue(
        carboy.fill(cloudy, FluidAction.EXECUTE) == 1000,
        "Portable bulk container transports unclarified batches");
    BlockPos pos = new BlockPos(2, 2, 2);
    helper.setBlock(pos, ModBlocks.FERMENTATION_VAT.get());
    var vat = (FermentationVesselBlockEntity) helper.getBlockEntity(pos);
    vat.getFluidHandler(null).fill(carboy.drain(1000, FluidAction.EXECUTE), FluidAction.EXECUTE);
    Component status = vat.getStatusMessage();
    helper.assertTrue(
        status.getContents() instanceof TranslatableContents text
            && text.getArgs()[0] instanceof Component name
            && name.equals(WineCatalog.name(cloudy)),
        "Vat identifies the actual variety awaiting clarification");
    helper.assertTrue(vat.press(), "Fermented wine can be clarified without losing volume");
    FluidStack clear = vat.getFluid();
    var glass =
        ((FluidContainerItem) ModItems.WINE_GLASS_EMPTY.get())
            .handler(new ItemStack(ModItems.WINE_GLASS_EMPTY.get()));
    helper.assertTrue(
        glass.fill(clear, FluidAction.EXECUTE) == 125,
        "Clarified wine is accepted into a serving glass");
    helper.succeed();
  }
}
