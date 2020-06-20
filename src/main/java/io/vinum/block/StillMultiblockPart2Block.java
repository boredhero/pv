/*
    Project Vinum - StillMultiblockPart2Block.java.java
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

import javax.annotation.Nullable;

import io.vinum.block.state.properties.ModBlockStateProperties;
import io.vinum.tileentity.StillSlaveTileEntity;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class StillMultiblockPart2Block extends ContainerBlock implements IWaterLoggable {
	
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final IntegerProperty PRESSURE = ModBlockStateProperties.PRESSURE;
	
	public StillMultiblockPart2Block(Block.Properties propertiesIn) {
		super(propertiesIn);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(WATERLOGGED, Boolean.FALSE).with(PRESSURE, 0));
		
	}
	
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		IWorld iworld = context.getWorld();
		BlockPos blockpos = context.getPos();
		boolean flag = iworld.getFluidState(blockpos).getFluid() == Fluids.WATER;
		
		return this.getDefaultState().with(WATERLOGGED, Boolean.valueOf(flag)).with(FACING, Direction.NORTH).with(FACING, context.getPlacementHorizontalFacing());
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return VoxelShapeHelper.addShapes(VoxelShapeHelper.createRotatableShape(state, 0.0D, 10.0D, 7.0D, 2.0D, 12.0D, 9.0D), Block.makeCuboidShape(2.0D, 0.0D, 3.0D, 14.0D, 14.0D, 13.0D), Block.makeCuboidShape(3.0D, 0.0D, 2.0D, 13.0D, 14.0D, 14.0D));
		
	}
	
	public boolean isTransparent(BlockState state) {
		
		return true;
		
	}
	
	public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
		
		return false;
		
	}
	
	public BlockRenderType getRenderType(BlockState state) {
		
		return BlockRenderType.MODEL;
		
	}
	
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		if (worldIn.isRemote) {
			
			return ActionResultType.SUCCESS;
			
		} else {
			
			this.interactWith(worldIn, pos, player);
			
			return ActionResultType.SUCCESS;
			
		}
		
	}
	
	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		
		return new StillSlaveTileEntity();
		
	}
	
	protected void interactWith(World worldIn, BlockPos pos, PlayerEntity player) {
		
		TileEntity tileentity = worldIn.getTileEntity(pos);
		
		if (tileentity instanceof StillSlaveTileEntity) {
			
			player.openContainer((INamedContainerProvider)((StillSlaveTileEntity) tileentity).getMasterTileEntity());
			//player.addStat(Stats.INTERACT_WITH_FURNACE);
			
		}
		
	}
	
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		
		if (stack.hasDisplayName()) {
			
			TileEntity tileentity = worldIn.getTileEntity(pos);
			
			if (tileentity instanceof StillSlaveTileEntity) {
				
				((StillSlaveTileEntity)tileentity).setCustomName(stack.getDisplayName());
				
			}
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public IFluidState getFluidState(BlockState state) {
		
		return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
		
	}
	
	public BlockState rotate(BlockState state, Rotation rot) {
		
		return state.with(FACING, rot.rotate(state.get(FACING)));
		
	}
	
	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		
		return state.rotate(mirrorIn.toRotation(state.get(FACING)));
	
	}
	
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		
		builder.add(WATERLOGGED, FACING, PRESSURE);
		
	}
	
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		
		return false;
		
	}
	
}