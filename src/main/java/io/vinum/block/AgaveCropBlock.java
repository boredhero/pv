package io.vinum.block;

import java.util.Random;

import io.vinum.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BushBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class AgaveCropBlock extends BushBlock implements IGrowable {
	
	public static final IntegerProperty AGE = BlockStateProperties.AGE_0_2;
	private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 11.0D, 16.0D), Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D)};
	
	
	protected AgaveCropBlock(Block.Properties builder) {
		super(builder);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(this.getAgeProperty(), Integer.valueOf(0)));
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return SHAPE_BY_AGE[state.get(this.getAgeProperty())];
		
	}
	
	protected boolean isValidGround(BlockState state, IBlockReader worldIn, BlockPos pos) {
		
		return state.getBlock() == Blocks.FARMLAND || state.getBlock().isIn(BlockTags.SAND);
		
	}
	
	public IntegerProperty getAgeProperty() {
		
		return AGE;
		
	}
	
	public int getMaxAge() {
		
		return 2;
		
	}
	
	protected int getAge(BlockState state) {
		
		return state.get(this.getAgeProperty());
		
	}
	
	public BlockState withAge(int age) {
		
		return this.getDefaultState().with(this.getAgeProperty(), Integer.valueOf(age));
		
	}
	
	public boolean isMaxAge(BlockState state) {
		
		return state.get(this.getAgeProperty()) >= this.getMaxAge();
		
	}
	
	@SuppressWarnings("deprecation")
	public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
		super.tick(state, worldIn, pos, rand);
		
		if (!worldIn.isAreaLoaded(pos, 1)) return;
		
		if (worldIn.getLightSubtracted(pos, 0) >= 9) {
			
			int i = this.getAge(state);
			
			if (i < this.getMaxAge()) {
				
				float f = getGrowthChance(this, worldIn, pos);
				
				if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state, rand.nextInt((int)(25.0F / f) + 1) == 0)) {
					
					worldIn.setBlockState(pos, this.withAge(i + 1), 2);
					net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state);
					
				}
				
			}
			
		}

	}
	
	public void grow(World worldIn, BlockPos pos, BlockState state) {
		
		int i = this.getAge(state) + this.getBonemealAgeIncrease(worldIn);
		int j = this.getMaxAge();
		
		if (i > j) {
			
			i = j;
			
		}

		worldIn.setBlockState(pos, this.withAge(i), 2);
		
	}
	
	protected int getBonemealAgeIncrease(World worldIn) {
		
		return MathHelper.nextInt(worldIn.rand, 2, 5);
		
	}
	
	protected static float getGrowthChance(Block blockIn, IBlockReader worldIn, BlockPos pos) {
		
		float f = 1.0F;
		BlockPos blockpos = pos.down();
		
		for (int i = -1; i <= 1; ++i) {
			
			for (int j = -1; j <= 1; ++j) {
				
				float f1 = 0.0F;
				BlockState blockstate = worldIn.getBlockState(blockpos.add(i, 0, j));
				
				if (blockstate.canSustainPlant(worldIn, blockpos.add(i, 0, j), net.minecraft.util.Direction.UP, (net.minecraftforge.common.IPlantable)blockIn)) {
					
					f1 = 1.0F;
					
					if (blockstate.isFertile(worldIn, blockpos.add(i, 0, j))) {
						
						f1 = 3.0F;
						
					}
					
				}
				
				if (i != 0 || j != 0) {
					
					f1 /= 4.0F;
					
				}
				
				f += f1;
				
			}
			
		}
		
		BlockPos blockpos1 = pos.north();
		BlockPos blockpos2 = pos.south();
		BlockPos blockpos3 = pos.west();
		BlockPos blockpos4 = pos.east();
		boolean flag = blockIn == worldIn.getBlockState(blockpos3).getBlock() || blockIn == worldIn.getBlockState(blockpos4).getBlock();
		boolean flag1 = blockIn == worldIn.getBlockState(blockpos1).getBlock() || blockIn == worldIn.getBlockState(blockpos2).getBlock();
		
		if (flag && flag1) {
			
			f /= 2.0F;
			
		} else {
			
			boolean flag2 = blockIn == worldIn.getBlockState(blockpos3.north()).getBlock() || blockIn == worldIn.getBlockState(blockpos4.north()).getBlock() || blockIn == worldIn.getBlockState(blockpos4.south()).getBlock() || blockIn == worldIn.getBlockState(blockpos3.south()).getBlock();
			
			if (flag2) {
				
				f /= 2.0F;
				
			}
			
		}
		
		return f;
		
	}
	
	public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
		
		return (worldIn.getLightSubtracted(pos, 0) >= 8 || worldIn.canSeeSky(pos)) && super.isValidPosition(state, worldIn, pos);
		
	}
	
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		
		if (entityIn instanceof RavagerEntity && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(worldIn, entityIn)) {
			
			worldIn.destroyBlock(pos, true, entityIn);
			
		}
		
		super.onEntityCollision(state, worldIn, pos, entityIn);
		
	}
	
	protected IItemProvider getSeedsItem() {
		
		return ModItems.AGAVE_SEEDS.get();
		
	}
	
	public ItemStack getItem(IBlockReader worldIn, BlockPos pos, BlockState state) {
		
		return new ItemStack(this.getSeedsItem());
		
	}
	
	public boolean canGrow(IBlockReader worldIn, BlockPos pos, BlockState state, boolean isClient) {
		
		return !this.isMaxAge(state);
		
	}
	
	public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, BlockState state) {
		
		return true;
		
	}
	
	public void grow(ServerWorld worldIn, Random rand, BlockPos pos, BlockState state) {
		
		this.grow(worldIn, pos, state);
		
	}
	
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		
		builder.add(AGE);
		
	}
	
}