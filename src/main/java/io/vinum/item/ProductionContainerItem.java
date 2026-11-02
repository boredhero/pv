package io.vinum.item;

import io.vinum.production.ProductionContainers;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A reusable bucket carrying an exact fluid quantity and its immutable batch properties. */
public final class ProductionContainerItem extends Item {
    public ProductionContainerItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        return ProductionContainers.read(stack)
                .<Component>map(fluid -> Component.translatable("item.vinum.batch_bucket.contents", fluid.getHoverName()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ProductionContainers.read(stack).ifPresent(fluid -> tooltip.add(
                Component.translatable("tooltip.vinum.batch_volume", fluid.getAmount()).withStyle(ChatFormatting.GRAY)));
    }
}
