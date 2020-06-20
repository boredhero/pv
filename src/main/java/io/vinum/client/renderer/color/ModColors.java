/*
    Project Vinum - ModColors.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
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