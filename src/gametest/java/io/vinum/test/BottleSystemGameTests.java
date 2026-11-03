package io.vinum.test;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.*;
import io.vinum.config.BarrelConfig;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.production.BatchProfile;
import java.util.List;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BottleSystemGameTests {
  @GameTest(template = "empty")
  public static void typedAppearanceResolvesGeometryMaterialsAndInheritedStages(GameTestHelper h) {
    var bottle = new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get());
    var fluid = ModItems.spirit(ModFluids.TEQUILA.source().get(), 317);
    var original = fluid.copy();
    var base = BeverageAppearance.resolve(fluid, bottle);
    var aged = BeverageAppearance.resolve(BarrelFluidData.withAge(fluid,
        BarrelConfig.daysToTicks(BarrelConfig.tequilaAnejoDays())), bottle);
    h.assertTrue(base.bottle3DType().geometry().sides() == 4, "Tequila uses square geometry");
    h.assertTrue(base.bottleType().equals(aged.bottleType())
        && base.bottle3DType().equals(aged.bottle3DType())
        && base.glassMaterial().equals(aged.glassMaterial())
        && base.closure().equals(aged.closure()), "Age variants inherit all unmodified appearance fields");
    h.assertTrue(!base.label().equals(aged.label()), "Age selects a distinct label palette");
    h.assertTrue(base.closure().type() == BeverageAppearance.ClosureType.CAP
        && base.glassMaterial().color() >>> 24 < 255, "Closure and glass opacity come from typed data");
    var emptyWine = BeverageAppearance.resolve(net.neoforged.neoforge.fluids.FluidStack.EMPTY,
        new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get()));
    h.assertTrue(emptyWine.closure().type() == BeverageAppearance.ClosureType.CORK
        && !emptyWine.bottle3DType().equals(base.bottle3DType()), "Empty vessel retains its family defaults");
    h.assertTrue(net.neoforged.neoforge.fluids.FluidStack.isSameFluidSameComponents(fluid, original)
        && fluid.getAmount() == 317 && bottle.getComponents().equals(new ItemStack(bottle.getItem()).getComponents()),
        "Resolving appearance never changes quantity or components");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void reusableShotTransfersExactVolumeAndKeepsAgeAndAlcohol(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var bottle = (FluidContainerItem) ModItems.FIFTH_BOTTLE_EMPTY.get();
    var glass = (FluidContainerItem) ModItems.SHOT_GLASS.get();
    long age = BarrelConfig.daysToTicks(BarrelConfig.tequilaAnejoDays());
    var fluid =
        BarrelFluidData.withAge(ModItems.spirit(ModFluids.TEQUILA.source().get(), 750), age);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(glass));
    player.setItemInHand(InteractionHand.OFF_HAND, bottle.filled(fluid));
    h.assertTrue(
        ContainerTransfers.betweenHands(h.getLevel(), player, InteractionHand.MAIN_HAND),
        "Fill one shot from the other hand");
    var serving = glass.contents(player.getMainHandItem());
    h.assertTrue(
        serving.getAmount() == 50 && bottle.contents(player.getOffhandItem()).getAmount() == 700,
        "Exactly50mB move;700mB remain");
    h.assertTrue(
        BarrelFluidData.getAge(serving) == age
            && BatchProfile.of(serving).equals(BatchProfile.of(fluid)),
        "Transfer retains age, alcohol and ingredients");
    h.assertTrue(
        BeverageAppearance.find(serving)
            .orElseThrow()
            .name()
            .equals("beverage.vinum.tequila.anejo"),
        "Aged shot selects the aged presentation");
    ItemStack emptied = player.getMainHandItem().finishUsingItem(h.getLevel(), player);
    h.assertTrue(
        emptied.is(glass) && glass.contents(emptied).isEmpty(),
        "Drinking returns the same reusable shot glass");
    h.assertTrue(
        !io.vinum.tileentity.StillMasterTileEntity.isValidInput(1, player.getOffhandItem()),
        "Still rejects partially filled bottles rather than overwriting liquid");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void bottleAppearanceUsesAgeAndKeepsEmptyDefaultShape(GameTestHelper h) {
    String[] names = {"blanco", "reposado", "anejo", "extra_anejo"};
    int[] days = {
      0,
      BarrelConfig.tequilaReposadoDays(),
      BarrelConfig.tequilaAnejoDays(),
      BarrelConfig.tequilaExtraAnejoDays()
    };
    java.util.Set<Integer> labels = new java.util.HashSet<>();
    for (int i = 0; i < days.length; i++) {
      var fluid =
          BarrelFluidData.withAge(
              ModItems.spirit(ModFluids.TEQUILA.source().get(), 750),
              BarrelConfig.daysToTicks(days[i]));
      var style = BeverageAppearance.find(fluid).orElseThrow();
      h.assertTrue(
          style.bottleBaseType().equals("tequila") && style.name().endsWith(names[i]),
          "Catalog selects the tequila shape and precise aging class");
      labels.add(style.bottleLabelPrimaryColor());
      if (i > 0) {
        var prior = BarrelFluidData.withAge(fluid, BarrelConfig.daysToTicks(days[i]) - 1);
        h.assertTrue(
            BeverageAppearance.find(prior).orElseThrow().name().endsWith(names[i - 1]),
            "Class changes exactly at aging threshold");
      }
    }
    h.assertTrue(
        labels.size() == 4
            && BeverageAppearance.model(net.neoforged.neoforge.fluids.FluidStack.EMPTY) == 0,
        "Aging varies labels; empty bottle uses default shape");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void shotPackOpensWithoutLosingGlassInVacatedHandSlot(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(
        InteractionHand.MAIN_HAND, new ItemStack(ModItems.PACK_OF_SHOT_GLASSES.get()));
    var result = player.getMainHandItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
    player.setItemInHand(InteractionHand.MAIN_HAND, result.getObject());
    h.assertTrue(
        player.getInventory().countItem(ModItems.SHOT_GLASS.get()) == 8
            && player.getInventory().countItem(ModItems.PACK_OF_SHOT_GLASSES.get()) == 0,
        "One pack produces exactly eight empty glasses");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void cocktailRequiresRealTequilaAndReturnsReusableShotGlasses(GameTestHelper h) {
    var glass = (FluidContainerItem) ModItems.SHOT_GLASS.get();
    var shot = glass.filled(ModItems.spirit(ModFluids.TEQUILA.source().get(), 50));
    var inputs =
        new java.util.ArrayList<>(
            List.of(
                shot.copy(),
                shot.copy(),
                new ItemStack(ModItems.APPLE_JUICE.get()),
                new ItemStack(ModItems.SEA_SALT.get()),
                new ItemStack(ModItems.CINNAMON.get()),
                new ItemStack(Items.HONEY_BOTTLE),
                new ItemStack(ModItems.ICE_CUBES.get()),
                new ItemStack(ModItems.MARGARITA_GLASS.get()),
                new ItemStack(ModItems.COCKTAIL_SHAKER.get())));
    var crafting = CraftingInput.of(3, 3, inputs);
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, crafting, h.getLevel())
            .orElseThrow()
            .value();
    var remainder = recipe.getRemainingItems(crafting);
    h.assertTrue(
        remainder.get(0).is(glass)
            && remainder.get(1).is(glass)
            && glass.contents(remainder.get(0)).isEmpty()
            && glass.contents(remainder.get(1)).isEmpty(),
        "Both shots are consumed as fluid and leave reusable glasses");
    h.assertTrue(
        glass.contents(shot).getAmount() == 50,
        "Recipe matching and remainders do not mutate input stacks");
    inputs.set(0, new ItemStack(glass));
    h.assertTrue(
        !recipe.matches(CraftingInput.of(3, 3, inputs), h.getLevel()),
        "Empty shot cannot masquerade as tequila");
    inputs.set(
        0,
        glass.filled(
            new net.neoforged.neoforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.WATER, 50)));
    h.assertTrue(
        !recipe.matches(CraftingInput.of(3, 3, inputs), h.getLevel()),
        "Water cannot substitute for tequila");
    h.succeed();
  }
}
