package io.vinum.block;

import javax.annotation.Nullable;

import io.vinum.multiblock.StillMultiblock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class SteelPotBlock extends Block implements IWaterLoggable {
	
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	public SteelPotBlock(Block.Properties propertiesIn) {
		super(propertiesIn);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(WATERLOGGED, Boolean.FALSE));
		
	}
	
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		
		StillMultiblock.build(worldIn, pos, state, placer, stack);
		
	}
	
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		IWorld iworld = context.getWorld();
		BlockPos blockpos = context.getPos();
		boolean flag = iworld.getFluidState(blockpos).getFluid() == Fluids.WATER;
		
		return this.getDefaultState().with(WATERLOGGED, Boolean.valueOf(flag)).with(FACING, Direction.NORTH).with(FACING, context.getPlacementHorizontalFacing());
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return VoxelShapes.or(VoxelShapes.or(VoxelShapes.or(Block.makeCuboidShape(1.0D, 2.0D, 1.0D, 15.0D, 14.0D, 15.0D), Block.makeCuboidShape(3.0D, 1.0D, 3.0D, 13.0D, 2.0D, 13.0D)), VoxelShapes.or(VoxelShapes.or(Block.makeCuboidShape(4.0D, 1.0D, 2.0D, 12.0D, 2.0D, 3.0D), Block.makeCuboidShape(2.0D, 1.0D, 4.0D, 3.0D, 2.0D, 12.0D)), VoxelShapes.or(Block.makeCuboidShape(4.0D, 1.0D, 14.0D, 12.0D, 2.0D, 13.0D), Block.makeCuboidShape(14.0D, 1.0D, 4.0D, 13.0D, 2.0D, 12.0D))), VoxelShapes.or(VoxelShapes.or(Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 4.0D, 1.0D, 4.0D), Block.makeCuboidShape(14.0D, 0.0D, 2.0D, 12.0D, 1.0D, 4.0D)), VoxelShapes.or(Block.makeCuboidShape(2.0D, 0.0D, 14.0D, 4.0D, 1.0D, 12.0D), Block.makeCuboidShape(14.0D, 0.0D, 14.0D, 12.0D, 1.0D, 12.0D)))));
		
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
		
		builder.add(WATERLOGGED, FACING);
		
	}
	
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		
		return false;
		
	}
	
}