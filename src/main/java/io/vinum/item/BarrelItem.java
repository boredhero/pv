package io.vinum.item;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.config.BarrelConfig;
import java.math.BigDecimal;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

/** Placeable barrels remain reusable in the existing fermentation crafting recipes. */
public final class BarrelItem extends BlockItem {
    public BarrelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        // Persisted reservoirs must not multiply fluid through a stacked block placement.
        return stack.has(DataComponents.BLOCK_ENTITY_DATA) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.vinum.barrel_capacity", litres(BarrelConfig.capacityPerBlock()))
                .withStyle(ChatFormatting.GRAY));
        var stored = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (stored == null || context.registries() == null) return;
        FluidStack fluid = FluidStack.parseOptional(context.registries(), stored.copyTag().getCompound("Fluid"));
        if (fluid.isEmpty()) return;
        tooltip.add(Component.translatable("tooltip.vinum.barrel_contents", fluid.getHoverName(), litres(fluid.getAmount()))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.vinum.barrel_age", BarrelFluidData.getAge(fluid) / BarrelConfig.ticksPerDay())
                .withStyle(ChatFormatting.GRAY));
    }

    private static String litres(int millibuckets) {
        return BigDecimal.valueOf(millibuckets, 3).stripTrailingZeros().toPlainString();
    }
}
