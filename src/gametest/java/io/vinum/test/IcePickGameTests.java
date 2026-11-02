package io.vinum.test;

import io.vinum.item.IcePickItem;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class IcePickGameTests {
  @GameTest(template = "barrel_space")
  public static void icePickChipsWorldIceOnceAndWearsOnePoint(GameTestHelper h) {
    var pos = h.absolutePos(new BlockPos(2, 2, 2));
    h.getLevel().setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    ItemStack pick = new ItemStack(ModItems.ICE_PICK.get());
    player.setItemInHand(InteractionHand.MAIN_HAND, pick);
    var context =
        new UseOnContext(
            h.getLevel(),
            player,
            InteractionHand.MAIN_HAND,
            pick,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    h.assertTrue(pick.useOn(context).consumesAction(), "Ice is chipped by actual item interaction");
    h.assertTrue(
        h.getLevel().isEmptyBlock(pos) && pick.getDamageValue() == 1 && pick.getMaxDamage() == 512,
        "Consumes ice once and spends one of 512 durability points");
    int drops =
        h
            .getLevel()
            .getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, new AABB(pos).inflate(1))
            .stream()
            .filter(e -> e.getItem().is(ModItems.ICE_CUBES.get()))
            .mapToInt(e -> e.getItem().getCount())
            .sum();
    h.assertTrue(drops >= 1 && drops <= 3, "One ice block drops 1..3 cubes");
    h.assertTrue(
        !pick.useOn(context).consumesAction() && pick.getDamageValue() == 1,
        "Repeating on air cannot generate cubes or spend durability");
    h.succeed();
  }

  @GameTest(template = "barrel_space")
  public static void fortuneUsesOreMultiplierAndIcePickAcceptsToolEnchantments(GameTestHelper h) {
    RandomSource random = RandomSource.create(12345);
    boolean bonus = false;
    for (int i = 0; i < 1000; i++) {
      int base = IcePickItem.rollDrops(random, 0), fortune = IcePickItem.rollDrops(random, 3);
      h.assertTrue(
          base >= 1 && base <= 3 && fortune >= 1 && fortune <= 12,
          "Fortune III follows bounded ore multiplier");
      bonus |= fortune > 3;
    }
    h.assertTrue(bonus, "Fortune can increase drops above the ordinary maximum");
    var registry =
        h.getLevel()
            .registryAccess()
            .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
    ItemStack pick = new ItemStack(ModItems.ICE_PICK.get());
    for (var key :
        java.util.List.of(
            net.minecraft.world.item.enchantment.Enchantments.FORTUNE,
            net.minecraft.world.item.enchantment.Enchantments.UNBREAKING,
            net.minecraft.world.item.enchantment.Enchantments.MENDING))
      h.assertTrue(
          registry.getHolderOrThrow(key).value().isSupportedItem(pick),
          "Pick supports " + key.location());
    h.assertTrue(
        h.getLevel()
            .getRecipeManager()
            .byKey(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "vinum", "crafting/ice_cubes"))
            .isEmpty(),
        "Ice cube crafting shortcut is removed");
    h.succeed();
  }
}
