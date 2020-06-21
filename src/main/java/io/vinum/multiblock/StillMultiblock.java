/*
    Project Vinum - StillMultiblock.java
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
package io.vinum.multiblock;

import javax.annotation.Nullable;

import io.vinum.block.PVBlocks;
import io.vinum.tileentity.StillSlaveTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StillMultiblock {
	
	@SuppressWarnings("deprecation")
	public static void build(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		
		if (!worldIn.isRemote()) {
			
			Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
			
			for (Direction direction : directions) {
				
				if (stack.getItem() == Item.getItemFromBlock(PVBlocks.STEEL_BRAZIER.get())) {
					
					BlockPos topPot = pos.up();
					BlockPos bottomPot = pos.offset(direction.rotateYCCW());
					BlockPos coil = pos.offset(direction.rotateYCCW()).up();
					
					boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_2.get());
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasCoil = (worldIn.getBlockState(coil).getBlock() == PVBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_3.get());
					
					if (hasTopPot && hasBottomPot && hasCoil) {
						
						worldIn.setBlockState(pos, PVBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(topPot, PVBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(coil, PVBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, PVBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
						if (worldIn.getTileEntity(topPot) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(topPot)).setMasterTileEntity(worldIn.getTileEntity(pos));
							
						}
						
						if (worldIn.getTileEntity(coil) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(coil)).setMasterTileEntity(worldIn.getTileEntity(pos));
							
						}
						
						if (worldIn.getTileEntity(bottomPot) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(bottomPot)).setMasterTileEntity(worldIn.getTileEntity(pos));
							
						}
						
					}
					
				} else if (stack.getItem() == Item.getItemFromBlock(PVBlocks.STEEL_COIL.get())) {
					
					BlockPos topPot = pos.offset(direction.rotateY());
					BlockPos bottomPot = pos.down();
					BlockPos brazier = pos.offset(direction.rotateY()).down();
					
					boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_2.get());
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasBrazier = (worldIn.getBlockState(brazier).getBlock() == PVBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_1.get());
					
					if (hasTopPot && hasBottomPot && hasBrazier) {
						
						worldIn.setBlockState(brazier, PVBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(topPot, PVBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(pos, PVBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, PVBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
						if (worldIn.getTileEntity(topPot) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(topPot)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
						if (worldIn.getTileEntity(pos) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(pos)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
						if (worldIn.getTileEntity(bottomPot) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(bottomPot)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
					}
					
				} else if (stack.getItem() == Item.getItemFromBlock(PVBlocks.STEEL_POT.get())) {
					
					BlockPos bottomPot = pos.offset(direction.rotateYCCW()).down();
					BlockPos coil = pos.offset(direction.rotateYCCW());
					BlockPos brazier = pos.down();
					
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasCoil = (worldIn.getBlockState(coil).getBlock() == PVBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_3.get());
					boolean hasBrazier = (worldIn.getBlockState(brazier).getBlock() == PVBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_1.get());
					
					if (hasBottomPot && hasCoil && hasBrazier) {
						
						worldIn.setBlockState(brazier, PVBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(pos, PVBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(coil, PVBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, PVBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
						if (worldIn.getTileEntity(pos) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(pos)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
						if (worldIn.getTileEntity(coil) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(coil)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
						if (worldIn.getTileEntity(bottomPot) instanceof StillSlaveTileEntity) {
							
							((StillSlaveTileEntity) worldIn.getTileEntity(bottomPot)).setMasterTileEntity(worldIn.getTileEntity(brazier));
							
						}
						
					} else {
						
						BlockPos topPot = pos.offset(direction.rotateY()).up();
						coil = pos.up();
						brazier = pos.offset(direction.rotateY());
						
						boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == PVBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_2.get());
						hasCoil = (worldIn.getBlockState(coil).getBlock() == PVBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_3.get());
						hasBrazier = (worldIn.getBlockState(brazier).getBlock() == PVBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_1.get());
						
						if (hasTopPot && hasCoil && hasBrazier) {
							
							worldIn.setBlockState(brazier, PVBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(topPot, PVBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(coil, PVBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(pos, PVBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							
							if (worldIn.getTileEntity(topPot) instanceof StillSlaveTileEntity) {
								
								((StillSlaveTileEntity) worldIn.getTileEntity(topPot)).setMasterTileEntity(worldIn.getTileEntity(brazier));
								
							}
							
							if (worldIn.getTileEntity(coil) instanceof StillSlaveTileEntity) {
								
								((StillSlaveTileEntity) worldIn.getTileEntity(coil)).setMasterTileEntity(worldIn.getTileEntity(brazier));
								
							}
							
							if (worldIn.getTileEntity(pos) instanceof StillSlaveTileEntity) {
								
								((StillSlaveTileEntity) worldIn.getTileEntity(pos)).setMasterTileEntity(worldIn.getTileEntity(brazier));
								
							}
							
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
}