package io.vinum.sugar;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

public final class SugarKettleItem extends BlockItem {
    public SugarKettleItem(Block block,Properties properties){super(block,properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.vinum.sugar_kettle_controls").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.vinum.sugar_kettle_heat").withStyle(ChatFormatting.GRAY));
        var saved=stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if(saved!=null&&context.registries()!=null){
            var fluid=FluidStack.parseOptional(context.registries(),saved.copyTag().getCompound("Fluid"));
            if(!fluid.isEmpty())lines.add(Component.translatable("tooltip.vinum.vessel_contents",fluid.getHoverName(),fluid.getAmount()).withStyle(ChatFormatting.GOLD));
        }
    }
}
