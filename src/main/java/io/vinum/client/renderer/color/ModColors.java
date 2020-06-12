package io.vinum.client.renderer.color;

import io.vinum.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.ILightReader;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModColors {
	
	public static void init() {
		
		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        ItemColors itemColors = Minecraft.getInstance().getItemColors();
		
        blockColors.register((state, world, blockPos, tintIndex) -> {
			
			return world != null && blockPos != null ? BiomeColors.getFoliageColor(world, blockPos) : FoliageColors.getDefault();
			
		}, ModBlocks.CINNAMON_LEAVES.get());
        
        itemColors.register((stack, tintIndex) -> {
			
			BlockState blockstate = ((BlockItem)stack.getItem()).getBlock().getDefaultState();
			
			return blockColors.getColor(blockstate, (ILightReader)null, (BlockPos)null, tintIndex);
			
		}, ModBlocks.CINNAMON_LEAVES.get());
		
	}
	
}