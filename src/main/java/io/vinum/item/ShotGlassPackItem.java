package io.vinum.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Packaging is inventory convenience; opening it creates eight empty reusable glasses. */
public final class ShotGlassPackItem extends Item {
  public ShotGlassPackItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack pack = player.getItemInHand(hand);
    if (!level.isClientSide) {
      if (!player.hasInfiniteMaterials()) pack.shrink(1);
      int remaining = 8;
      if (pack.isEmpty()) {
        player.setItemInHand(hand, new ItemStack(ModItems.SHOT_GLASS.get()));
        remaining--;
      }
      for (int i = 0; i < remaining; i++) {
        ItemStack glass = new ItemStack(ModItems.SHOT_GLASS.get());
        if (!player.getInventory().add(glass)) player.drop(glass, false);
      }
    }
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("tooltip.vinum.shot_pack"));
  }
}
