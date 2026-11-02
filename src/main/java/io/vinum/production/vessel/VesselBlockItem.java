package io.vinum.production.vessel;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

public final class VesselBlockItem extends BlockItem {
    public VesselBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public int getMaxStackSize(ItemStack stack) { return stack.has(DataComponents.BLOCK_ENTITY_DATA) ? 1 : super.getMaxStackSize(stack); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable(getBlock() instanceof CarboyBlock ? "tooltip.vinum.carboy" : "tooltip.vinum.fermentation_vat").withStyle(ChatFormatting.GRAY));
        var saved = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (saved != null && context.registries() != null) {
            FluidStack fluid = FluidStack.parseOptional(context.registries(), saved.copyTag().getCompound("Fluid"));
            if (!fluid.isEmpty()) lines.add(Component.translatable("tooltip.vinum.vessel_contents", fluid.getHoverName(), fluid.getAmount()).withStyle(ChatFormatting.GOLD));
        }
    }
}
