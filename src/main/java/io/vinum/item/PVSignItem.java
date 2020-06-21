/*
    Project Vinum - PVSignItem.java
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
package io.vinum.item;

import javax.annotation.Nullable;

import io.vinum.gui.screen.PVEditSignScreen;
import io.vinum.tileentity.PVSignTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WallOrFloorItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class PVSignItem extends WallOrFloorItem {
	
	public PVSignItem(Properties propertiesIn, Block floorBlockIn, Block wallBlockIn) {
		super(floorBlockIn, wallBlockIn, propertiesIn);
		
	}
	
	@Override
	protected boolean onBlockPlaced(BlockPos pos, World worldIn, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		
		boolean flag = super.onBlockPlaced(pos, worldIn, player, stack, state);
		
		if (worldIn.isRemote && !flag && player != null) {
			
			((PVSignTileEntity) worldIn.getTileEntity(pos)).setPlayer(player);
			Minecraft.getInstance().displayGuiScreen(new PVEditSignScreen((PVSignTileEntity) worldIn.getTileEntity(pos)));
			
		}
		
		return flag;
		
	}
	
}