package io.vinum.test;

import com.mojang.serialization.JsonOps;
import io.vinum.block.*;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.ProductionInputs;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class RecipeAuditGameTests {
  @GameTest(template = "empty")
  public static void cinnamonPreservesToolComponentsAndAppliesWear(GameTestHelper h) {
    var recipe = crafting(h, "crafting/cinnamon");
    ItemStack shears = new ItemStack(Items.SHEARS);
    shears.setDamageValue(25);
    shears.set(DataComponents.CUSTOM_NAME, Component.literal("Kitchen shears"));
    var input = CraftingInput.of(2, 1, List.of(shears, new ItemStack(Items.SPRUCE_LOG)));
    h.assertTrue(recipe.matches(input, h.getLevel()), "Actual recipe accepts shears and spruce");
    var returned = recipe.getRemainingItems(input).getFirst();
    h.assertTrue(
        returned.is(Items.SHEARS)
            && returned.getCount() == 1
            && returned.getDamageValue() == 26
            && returned.getHoverName().equals(shears.getHoverName()),
        "Craft returns the named tool with one point of durability wear");
    h.assertTrue(shears.getDamageValue() == 25, "Remainder computation never mutates source stack");
    shears.setDamageValue(shears.getMaxDamage() - 1);
    h.assertTrue(
        recipe.getRemainingItems(input).getFirst().isEmpty(),
        "Exhausted shears break instead of duplicating");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void juiceAndSandWashingReturnContainers(GameTestHelper h) {
    var washing = crafting(h, "crafting/refined_sand");
    var input =
        CraftingInput.of(
            2, 1, List.of(new ItemStack(Items.SAND), new ItemStack(Items.WATER_BUCKET)));
    h.assertTrue(
        washing.matches(input, h.getLevel())
            && washing.getRemainingItems(input).get(1).is(Items.BUCKET),
        "Washing sand returns the water bucket");
    ItemStack juice = new ItemStack(ModItems.APPLE_JUICE.get());
    h.assertTrue(
        juice.getCraftingRemainingItem().is(Items.GLASS_BOTTLE),
        "Cocktail crafting returns the juice bottle");
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    h.assertTrue(
        juice.finishUsingItem(h.getLevel(), player).is(Items.GLASS_BOTTLE),
        "Drinking the last juice returns its bottle");
    ItemStack stacked = new ItemStack(ModItems.APPLE_JUICE.get(), 2);
    ItemStack remaining = stacked.finishUsingItem(h.getLevel(), player);
    h.assertTrue(
        remaining.is(ModItems.APPLE_JUICE.get())
            && remaining.getCount() == 1
            && player.getInventory().countItem(Items.GLASS_BOTTLE) == 1,
        "Drinking from a stack preserves other juice and gives exactly one bottle");
    h.succeed();
  }

  @GameTest(template = "empty")
  public static void glassMaterialsHaveAnUnbrokenRecipeChain(GameTestHelper h) {
    ItemStack lead = cook(h, "smelting/lead_ingot", new ItemStack(ModItems.BLOCK_LEAD_ORE.get()));
    ItemStack leadDust = craft(h, "crafting/lead_powder", lead);
    h.assertTrue(leadDust.getCount() == 9, "One lead ingot provides nine powder portions");
    ItemStack refined =
        craft(
            h,
            "crafting/refined_sand",
            new ItemStack(Items.SAND),
            new ItemStack(Items.WATER_BUCKET));
    ItemStack leaded = craft(h, "crafting/leaded_sand", refined, leadDust.copyWithCount(1));
    h.assertTrue(
        cook(h, "smelting/fine_crystal_glass", leaded).is(ModItems.BLOCK_FINE_CRYSTAL_GLASS.get()),
        "Lead and washed sand reach crystal glass");
    ItemStack ironDust = craft(h, "crafting/iron_powder", new ItemStack(Items.IRON_NUGGET));
    ItemStack ferrous = craft(h, "crafting/ferrous_sand", refined, ironDust);
    ItemStack dark = cook(h, "smelting/dark_wine_glass", ferrous);
    h.assertTrue(
        dark.is(ModItems.BLOCK_DARK_WINE_GLASS.get()),
        "Iron and washed sand reach dark wine glass");
    var pack = crafting(h, "crafting/block_lead");
    var nine =
        CraftingInput.of(
            3, 3, java.util.stream.IntStream.range(0, 9).mapToObj(i -> lead.copy()).toList());
    h.assertTrue(pack.matches(nine, h.getLevel()), "Lead ingots pack into a storage block");
    ItemStack unpacked =
        craft(h, "crafting/lead_ingots", pack.assemble(nine, h.getLevel().registryAccess()));
    h.assertTrue(
        unpacked.is(ModItems.LEAD_INGOT.get()) && unpacked.getCount() == 9,
        "Lead storage round trip preserves nine ingots");
    var fifth = crafting(h, "crafting/fifth_bottle_empty");
    var bottleInput =
        CraftingInput.of(
            3,
            3,
            List.of(
                new ItemStack(ModItems.STEEL_NUGGET.get()),
                dark.copy(),
                new ItemStack(ModItems.STEEL_NUGGET.get()),
                dark.copy(),
                dark.copy(),
                dark.copy(),
                dark.copy(),
                dark.copy(),
                dark.copy()));
    h.assertTrue(
        fifth.matches(bottleInput, h.getLevel()),
        "Custom glass joins common glass tag used by bottles");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void saltNeedsHeatReturnsBucketPausesAndCannotBeCollectedTwice(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos pos = h.absolutePos(new BlockPos(3, 3, 3));
    level.setBlockAndUpdate(
        pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
    level.setBlockAndUpdate(pos, ModBlocks.STEEL_POT.get().defaultBlockState());
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
    var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    level
        .getBlockState(pos)
        .useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
    h.assertTrue(
        player.getMainHandItem().is(Items.WATER_BUCKET)
            && SteelPotBlock.isPotEmpty(level.getBlockState(pos)),
        "Cold pot consumes nothing");
    level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
    level
        .getBlockState(pos)
        .useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, hit);
    h.assertTrue(
        player.getMainHandItem().is(Items.BUCKET)
            && level.getBlockState(pos).getValue(SteelPotBlock.EVAPORATION) == 1,
        "Starting evaporation immediately returns the bucket");
    for (int i = 0; i < 40; i++) level.getBlockState(pos).tick(level, pos, level.random);
    level.setBlockAndUpdate(
        pos.below(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
    for (int i = 0; i < 30; i++) level.getBlockState(pos).tick(level, pos, level.random);
    h.assertTrue(
        level.getBlockState(pos).getValue(SteelPotBlock.EVAPORATION) == 41,
        "Cold intervals do not advance evaporation");
    level.setBlockAndUpdate(pos.below(), Blocks.CAMPFIRE.defaultBlockState());
    for (int i = 0; i < 60; i++) level.getBlockState(pos).tick(level, pos, level.random);
    BlockState ready = level.getBlockState(pos);
    h.assertTrue(
        ready.getValue(SteelPotBlock.EVAPORATION) == SteelPotBlock.SALT_READY,
        "Exactly100heated ticks produce salt");
    var encoded = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, ready).getOrThrow();
    h.assertTrue(
        BlockState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(ready),
        "Ready salt persists in saved block state");
    var drops = Block.getDrops(ready, level, pos, null);
    h.assertTrue(
        drops.stream()
                    .filter(s -> s.is(ModItems.SEA_SALT.get()))
                    .mapToInt(ItemStack::getCount)
                    .sum()
                == 1
            && drops.stream()
                    .filter(s -> s.is(ModItems.STEEL_POT.get()))
                    .mapToInt(ItemStack::getCount)
                    .sum()
                == 1,
        "Breaking a ready pot recovers one salt and one pot");
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    level.getBlockState(pos).useWithoutItem(level, player, hit);
    level.getBlockState(pos).useWithoutItem(level, player, hit);
    h.assertTrue(
        player.getInventory().countItem(ModItems.SEA_SALT.get()) == 1
            && SteelPotBlock.isPotEmpty(level.getBlockState(pos)),
        "Collecting resets the pot and cannot duplicate salt");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void stillAssemblyCannotConsumeEvaporatingOrReadyPots(GameTestHelper h) {
    var level = h.getLevel();
    BlockPos master = h.absolutePos(new BlockPos(4, 2, 4));
    Direction facing = Direction.NORTH;
    level.setBlockAndUpdate(master, ModBlocks.STEEL_BRAZIER.get().defaultBlockState());
    level.setBlockAndUpdate(
        StillMultiblock.position(master, facing, 3),
        ModBlocks.STEEL_COIL.get().defaultBlockState());
    level.setBlockAndUpdate(
        StillMultiblock.position(master, facing, 4), ModBlocks.STEEL_POT.get().defaultBlockState());
    BlockPos pot = master.above();
    for (int progress : new int[] {1, SteelPotBlock.SALT_READY}) {
      level.setBlockAndUpdate(
          pot,
          ModBlocks.STEEL_POT
              .get()
              .defaultBlockState()
              .setValue(SteelPotBlock.EVAPORATION, progress));
      h.assertTrue(
          !StillMultiblock.build(level, pot)
              && level.getBlockState(pot).getValue(SteelPotBlock.EVAPORATION) == progress,
          "Assembly preserves pending water and salt");
    }
    level.setBlockAndUpdate(pot, ModBlocks.STEEL_POT.get().defaultBlockState());
    h.assertTrue(StillMultiblock.build(level, pot), "Empty pots still assemble normally");
    h.succeed();
  }

  private static CraftingRecipe crafting(GameTestHelper h, String id) {
    return (CraftingRecipe)
        h.getLevel().getRecipeManager().byKey(ProductionInputs.id(id)).orElseThrow().value();
  }

  private static ItemStack craft(GameTestHelper h, String id, ItemStack... ingredients) {
    var recipe = crafting(h, id);
    var input = CraftingInput.of(ingredients.length, 1, List.of(ingredients));
    h.assertTrue(recipe.matches(input, h.getLevel()), "Recipe reachable: " + id);
    return recipe.assemble(input, h.getLevel().registryAccess());
  }

  private static ItemStack cook(GameTestHelper h, String id, ItemStack ingredient) {
    var recipe =
        (AbstractCookingRecipe)
            h.getLevel().getRecipeManager().byKey(ProductionInputs.id(id)).orElseThrow().value();
    var input = new SingleRecipeInput(ingredient);
    h.assertTrue(recipe.matches(input, h.getLevel()), "Cooking recipe reachable: " + id);
    return recipe.assemble(input, h.getLevel().registryAccess());
  }
}
