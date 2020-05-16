package io.vinum.block;

import javax.annotation.Nullable;

import io.vinum.tileentity.StillMasterTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
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
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class StillMultiblockPart1Block extends ContainerBlock implements IWaterLoggable {
	
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	
	public StillMultiblockPart1Block(Block.Properties propertiesIn) {
		super(propertiesIn);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(WATERLOGGED, Boolean.FALSE));
		
	}
	
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		if (worldIn.isRemote) {
			
			return ActionResultType.SUCCESS;
			
		} else {
			
			this.interactWith(worldIn, pos, player);
			
			return ActionResultType.SUCCESS;
			
		}
		
	}
	
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		IWorld iworld = context.getWorld();
		BlockPos blockpos = context.getPos();
		boolean flag = iworld.getFluidState(blockpos).getFluid() == Fluids.WATER;
		
		return this.getDefaultState().with(WATERLOGGED, Boolean.valueOf(flag)).with(FACING, Direction.NORTH).with(FACING, context.getPlacementHorizontalFacing());
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return VoxelShapes.or(Block.makeCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), Block.makeCuboidShape(0.0D, 1.0D, 0.0D, 16.0D, 15.0D, 16.0D));
		
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
	
	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		
		return new StillMasterTileEntity();
		
	}
	
	protected void interactWith(World worldIn, BlockPos pos, PlayerEntity player) {
		
		TileEntity tileentity = worldIn.getTileEntity(pos);
		
		if (tileentity instanceof StillMasterTileEntity) {
			
			player.openContainer((INamedContainerProvider)tileentity);
			//player.addStat(Stats.INTERACT_WITH_FURNACE);
			
		}
		
	}
	
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		
		if (stack.hasDisplayName()) {
			
			TileEntity tileentity = worldIn.getTileEntity(pos);
			
			if (tileentity instanceof StillMasterTileEntity) {
				
				((StillMasterTileEntity)tileentity).setCustomName(stack.getDisplayName());
				
			}
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
		
		if (state.getBlock() != newState.getBlock()) {
			
			TileEntity tileentity = worldIn.getTileEntity(pos);
			
			if (tileentity instanceof StillMasterTileEntity) {
				
				InventoryHelper.dropInventoryItems(worldIn, pos, (StillMasterTileEntity)tileentity);
				worldIn.updateComparatorOutputLevel(pos, this);
				
			}
			
			super.onReplaced(state, worldIn, pos, newState, isMoving);
			
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
		
		builder.add(WATERLOGGED, FACING);
		
	}
	
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		
		return false;
		
	}
	
}