package io.vinum.test;

import io.vinum.tree.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class CorkWoodGameTests {
  @GameTest(template = "barrel_space")
  public static void corkWoodRecipesFuelAndVanillaCraftingWork(GameTestHelper h) {
    var level = h.getLevel();
    var planksRecipe =
        level
            .getRecipeManager()
            .getRecipeFor(
                RecipeType.CRAFTING,
                CraftingInput.of(1, 1, List.of(new ItemStack(CorkOakContent.LOG.get()))),
                level)
            .orElseThrow();
    var planks =
        planksRecipe
            .value()
            .assemble(
                CraftingInput.of(1, 1, List.of(new ItemStack(CorkOakContent.LOG.get()))),
                level.registryAccess());
    h.assertTrue(
        planks.is(CorkWoodContent.PLANKS.get().asItem()) && planks.getCount() == 4,
        "Cork logs supply four usable planks");
    var sticksInput =
        CraftingInput.of(1, 2, List.of(planks.copyWithCount(1), planks.copyWithCount(1)));
    var sticks =
        level
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, sticksInput, level)
            .orElseThrow()
            .value()
            .assemble(sticksInput, level.registryAccess());
    h.assertTrue(
        sticks.is(Items.STICK) && sticks.getCount() == 4,
        "Cork planks participate in vanilla wood-tag recipes");
    for (var block :
        List.of(
            CorkWoodContent.PLANKS,
            CorkWoodContent.STAIRS,
            CorkWoodContent.SLAB,
            CorkWoodContent.FENCE,
            CorkWoodContent.FENCE_GATE,
            CorkWoodContent.DOOR,
            CorkWoodContent.TRAPDOOR,
            CorkWoodContent.BUTTON,
            CorkWoodContent.PRESSURE_PLATE)) {
      h.assertTrue(
          level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
              .anyMatch(
                  r -> r.value().getResultItem(level.registryAccess()).is(block.get().asItem())),
          "Every cork construction block has a crafting recipe");
      h.assertTrue(
          new ItemStack(block.get()).getBurnTime(RecipeType.SMELTING) > 0,
          "Wooden construction items retain normal furnace fuel behavior: " + block.get());
    }
    h.assertTrue(
        CorkWoodContent.PLANKS
                .get()
                .defaultBlockState()
                .getFlammability(level, h.absolutePos(new BlockPos(2, 1, 2)), Direction.UP)
            > 0,
        "Cork planks burn like other wood");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void corkSlabsAndDoorsUseNormalDropRules(GameTestHelper h) {
    BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
    var doubleSlab =
        CorkWoodContent.SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
    h.assertTrue(
        Block.getDrops(doubleSlab, h.getLevel(), pos, null).stream()
                .filter(s -> s.is(CorkWoodContent.SLAB.get().asItem()))
                .mapToInt(ItemStack::getCount)
                .sum()
            == 2,
        "A double slab returns two slabs");
    var door = CorkWoodContent.DOOR.get().defaultBlockState();
    h.assertTrue(
        Block.getDrops(
                    door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), h.getLevel(), pos, null)
                .stream()
                .filter(s -> s.is(CorkWoodContent.DOOR.get().asItem()))
                .mapToInt(ItemStack::getCount)
                .sum()
            == 1,
        "The lower door owns exactly one item drop");
    h.assertTrue(
        Block.getDrops(
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                h.getLevel(),
                pos.above(),
                null)
            .isEmpty(),
        "The upper door cannot duplicate its item");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void harvestedCorkPickBlockKeepsRedAppearanceAndRegrowthDeadline(GameTestHelper h) {
    BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
    h.getLevel().setBlockAndUpdate(pos, CorkOakContent.LOG.get().defaultBlockState());
    var bark = (CorkLogBlockEntity) h.getLevel().getBlockEntity(pos);
    h.assertTrue(bark.harvest(), "Fresh cork can be peeled");
    long deadline = bark.readyAt();
    var clientCopy = new CorkLogBlockEntity(pos, bark.getBlockState());
    clientCopy.loadWithComponents(
        bark.getUpdateTag(h.getLevel().registryAccess()), h.getLevel().registryAccess());
    h.assertTrue(
        clientCopy.readyAt() == deadline && bark.getUpdatePacket() != null,
        "Client synchronization includes the deadline needed by harvested pick-block artwork");
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    var picked =
        CorkOakContent.LOG
            .get()
            .getCloneItemStack(h.getLevel().getBlockState(pos), hit, h.getLevel(), pos, player);
    h.assertTrue(
        CorkLogItem.harvested(picked), "Picked peeled logs select the red harvested item model");
    h.assertTrue(
        !CorkLogItem.harvested(new ItemStack(CorkOakContent.LOG.get())),
        "Fresh logs retain their cork-covered item model");
    BlockPos ground = pos.offset(2, -1, 0);
    h.getLevel().setBlockAndUpdate(ground, Blocks.DIRT.defaultBlockState());
    player.setItemInHand(InteractionHand.MAIN_HAND, picked);
    var placeHit =
        new BlockHitResult(Vec3.atCenterOf(ground).add(0, .5, 0), Direction.UP, ground, false);
    h.assertTrue(
        picked
            .useOn(
                new UseOnContext(h.getLevel(), player, InteractionHand.MAIN_HAND, picked, placeHit))
            .consumesAction(),
        "Picked log places normally");
    var placed = (CorkLogBlockEntity) h.getLevel().getBlockEntity(ground.above());
    h.assertTrue(
        placed != null
            && placed.readyAt() == deadline
            && placed.getBlockState().getValue(CorkLogBlock.HARVESTED),
        "Placement keeps the red state and original bark regrowth deadline");
    h.succeed();
  }
}
