package io.vinum.multiblock;

import javax.annotation.Nullable;

import io.vinum.block.ModBlocks;
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
				
				if (stack.getItem() == Item.getItemFromBlock(ModBlocks.STEEL_BRAZIER.get())) {
					
					BlockPos topPot = pos.up();
					BlockPos bottomPot = pos.offset(direction.rotateYCCW());
					BlockPos coil = pos.offset(direction.rotateYCCW()).up();
					
					boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_2.get());
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasCoil = (worldIn.getBlockState(coil).getBlock() == ModBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_3.get());
					
					if (hasTopPot && hasBottomPot && hasCoil) {
						
						worldIn.setBlockState(pos, ModBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(topPot, ModBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(coil, ModBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, ModBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
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
					
				} else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.STEEL_COIL.get())) {
					
					BlockPos topPot = pos.offset(direction.rotateY());
					BlockPos bottomPot = pos.down();
					BlockPos brazier = pos.offset(direction.rotateY()).down();
					
					boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_2.get());
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasBrazier = (worldIn.getBlockState(brazier).getBlock() == ModBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_1.get());
					
					if (hasTopPot && hasBottomPot && hasBrazier) {
						
						worldIn.setBlockState(brazier, ModBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(topPot, ModBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(pos, ModBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, ModBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
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
					
				} else if (stack.getItem() == Item.getItemFromBlock(ModBlocks.STEEL_POT.get())) {
					
					BlockPos bottomPot = pos.offset(direction.rotateYCCW()).down();
					BlockPos coil = pos.offset(direction.rotateYCCW());
					BlockPos brazier = pos.down();
					
					boolean hasBottomPot = (worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(bottomPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_4.get());
					boolean hasCoil = (worldIn.getBlockState(coil).getBlock() == ModBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_3.get());
					boolean hasBrazier = (worldIn.getBlockState(brazier).getBlock() == ModBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_1.get());
					
					if (hasBottomPot && hasCoil && hasBrazier) {
						
						worldIn.setBlockState(brazier, ModBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(pos, ModBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(coil, ModBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						worldIn.setBlockState(bottomPot, ModBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
						
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
						
						boolean hasTopPot = (worldIn.getBlockState(topPot).getBlock() == ModBlocks.STEEL_POT.get() || worldIn.getBlockState(topPot).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_2.get());
						hasCoil = (worldIn.getBlockState(coil).getBlock() == ModBlocks.STEEL_COIL.get() || worldIn.getBlockState(coil).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_3.get());
						hasBrazier = (worldIn.getBlockState(brazier).getBlock() == ModBlocks.STEEL_BRAZIER.get() || worldIn.getBlockState(brazier).getBlock() == ModBlocks.STILL_MULTIBLOCK_PART_1.get());
						
						if (hasTopPot && hasCoil && hasBrazier) {
							
							worldIn.setBlockState(brazier, ModBlocks.STILL_MULTIBLOCK_PART_1.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(topPot, ModBlocks.STILL_MULTIBLOCK_PART_2.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(coil, ModBlocks.STILL_MULTIBLOCK_PART_3.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							worldIn.setBlockState(pos, ModBlocks.STILL_MULTIBLOCK_PART_4.get().getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, direction));
							
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