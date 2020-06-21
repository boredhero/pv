/*
    Project Vinum - PVStandingSignBlock.java
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
package io.vinum.block;

import io.vinum.tileentity.PVSignTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.StandingSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class PVStandingSignBlock extends StandingSignBlock {
	
	public PVStandingSignBlock(Properties properties, WoodType wood) {
		super(properties, wood);
		
	}
	
	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		
		return new PVSignTileEntity();
		
	}
	
	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		ItemStack itemstack = player.getHeldItem(handIn);
		boolean flag = itemstack.getItem() instanceof DyeItem && player.abilities.allowEdit;
		if (worldIn.isRemote) {
			
			return flag ? ActionResultType.SUCCESS : ActionResultType.CONSUME;
			
		} else {
			
			TileEntity tileentity = worldIn.getTileEntity(pos);
			
			if (tileentity instanceof PVSignTileEntity) {
				
				PVSignTileEntity signtileentity = (PVSignTileEntity)tileentity;
				
				if (flag) {
					
					boolean flag1 = signtileentity.setTextColor(((DyeItem)itemstack.getItem()).getDyeColor());
					
					if (flag1 && !player.isCreative()) {
						
						itemstack.shrink(1);
						
					}
					
				}
				
				return signtileentity.executeCommand(player) ? ActionResultType.SUCCESS : ActionResultType.PASS;
				
			} else {
				
				return ActionResultType.PASS;
				
			}
			
		}
		
	}
	
}