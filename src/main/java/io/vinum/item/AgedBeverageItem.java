package io.vinum.item;

import java.util.List;
import io.vinum.barrel.BeverageCatalog;
import io.vinum.config.BarrelConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Bottled age is a snapshot; only time spent inside an assembled barrel increases maturity. */
public class AgedBeverageItem extends Item {
    public AgedBeverageItem(Properties properties) { super(properties); }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        appendAgeTooltip(stack, tooltip);
    }
    public static void appendAgeTooltip(ItemStack stack, List<Component> tooltip) {
        BeverageCatalog.lookupFilled(stack).ifPresent(match -> {
            long age = match.serving().age(stack);
            tooltip.add(Component.translatable("tooltip.vinum.barrel_age", age / BarrelConfig.ticksPerDay()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.vinum.maturation", Component.translatable(match.beverage().aging().stage(age)))
                    .withStyle(ChatFormatting.GOLD));
        });
    }
}
