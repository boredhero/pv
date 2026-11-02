package io.vinum.item;

import io.vinum.block.AgaveCropBlock;
import io.vinum.block.AgaveLeafBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A jimador's long-handled blade: clip foliage, then harvest the exposed piña. */
public final class CoaItem extends Item {
  public static final int DURABILITY = 512;

  public CoaItem(Properties properties) {
    super(properties.durability(DURABILITY));
  }

  @Override
  public float getDestroySpeed(ItemStack stack, BlockState state) {
    return state.getBlock() instanceof AgaveCropBlock || state.getBlock() instanceof AgaveLeafBlock
        ? 6.0F : 1.0F;
  }

  @Override
  public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
      LivingEntity miner) {
    if (!level.isClientSide)
      stack.hurtAndBreak(1, miner, EquipmentSlot.MAINHAND);
    return true;
  }

  @Override
  public int getEnchantmentValue(ItemStack stack) { return 14; }

  @Override
  public boolean isValidRepairItem(ItemStack tool, ItemStack material) {
    return material.is(Items.IRON_INGOT);
  }
}
