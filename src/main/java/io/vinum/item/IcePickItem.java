package io.vinum.item;

import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Chips world ice once; server-side mutation and drops share the normal block-break protection
 * event.
 */
public final class IcePickItem extends Item {
  public static final int DURABILITY = 512;
  private static final TagKey<Block> CHIPPABLE =
      TagKey.create(
          Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Defines.MODID, "chippable_ice"));

  public IcePickItem(Properties properties) {
    super(properties.durability(DURABILITY));
  }

  @Override
  public int getEnchantmentValue(ItemStack stack) {
    return 14;
  }

  @Override
  public boolean isValidRepairItem(ItemStack tool, ItemStack material) {
    return material.is(ModItems.STEEL_INGOT.get());
  }

  /** Vanilla ore Fortune multiplier applied to an independent 1..3 base roll. */
  public static int rollDrops(RandomSource random, int fortune) {
    int base = 1 + random.nextInt(3);
    int multiplier =
        fortune <= 0 ? 1 : 1 + Math.max(0, random.nextInt(Math.min(fortune, 255) + 2) - 1);
    return base * multiplier;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var level = context.getLevel();
    var pos = context.getClickedPos();
    var state = level.getBlockState(pos);
    var player = context.getPlayer();
    var tool = context.getItemInHand();
    if (!state.is(CHIPPABLE)) return InteractionResult.PASS;
    if (player == null
        || !player.mayBuild()
        || !level.mayInteract(player, pos)
        || !player.mayUseItemAt(pos, context.getClickedFace(), tool)) return InteractionResult.FAIL;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled())
      return InteractionResult.FAIL;
    var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    int fortune = tool.getEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.FORTUNE));
    boolean silk =
        tool.getEnchantmentLevel(enchantments.getHolderOrThrow(Enchantments.SILK_TOUCH)) > 0;
    if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL))
      return InteractionResult.FAIL;
    level.levelEvent(2001, pos, Block.getId(state));
    if (!player.hasInfiniteMaterials()) {
      Block.popResource(
          level,
          pos,
          silk
              ? new ItemStack(state.getBlock())
              : new ItemStack(ModItems.ICE_CUBES.get(), rollDrops(level.random, fortune)));
      tool.hurtAndBreak(
          1,
          player,
          context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
              ? EquipmentSlot.MAINHAND
              : EquipmentSlot.OFFHAND);
    }
    player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    return InteractionResult.CONSUME;
  }
}
