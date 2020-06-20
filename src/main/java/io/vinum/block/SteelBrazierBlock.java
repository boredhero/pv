/*
    Project Vinum - SteelBrazierBlock.java
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

import java.util.Random;

import javax.annotation.Nullable;

import io.vinum.multiblock.StillMultiblock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SteelBrazierBlock extends Block implements IWaterLoggable {
	
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	
	public SteelBrazierBlock(Block.Properties propertiesIn) {
		super(propertiesIn);
		
		this.setDefaultState(this.stateContainer.getBaseState().with(WATERLOGGED, Boolean.FALSE).with(FACING, Direction.NORTH).with(LIT, Boolean.FALSE));
		
	}
	
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		
		StillMultiblock.build(worldIn, pos, state, placer, stack);
		
	}
	
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		
		if (!worldIn.isRemote()) {
			
			if (!state.get(LIT) && !state.get(WATERLOGGED)) {
				
				ItemStack itemstack = player.getHeldItem(handIn);
				
				if (player.isCreative()) {
					
					if (itemstack.getItem() instanceof FlintAndSteelItem) {
						
						worldIn.setBlockState(pos, state.with(LIT, Boolean.TRUE));
						player.swingArm(handIn);
						return ActionResultType.CONSUME;
						
					}
					
					if (itemstack.getItem() instanceof FireChargeItem) {
						
						worldIn.setBlockState(pos, state.with(LIT, Boolean.TRUE));
						player.swingArm(handIn);
						return ActionResultType.CONSUME;
						
					}
					
				} else {
					
					if (itemstack.getItem() instanceof FlintAndSteelItem) {
						
						worldIn.setBlockState(pos, state.with(LIT, Boolean.TRUE));
						player.swingArm(handIn);
						itemstack.damageItem(1, player, (damageAnimation) -> {
							damageAnimation.sendBreakAnimation(handIn);
			            });
						return ActionResultType.CONSUME;
						
					}
					
					if (itemstack.getItem() instanceof FireChargeItem) {
						
						worldIn.setBlockState(pos, state.with(LIT, Boolean.TRUE));
						player.swingArm(handIn);
						itemstack.setCount(itemstack.getCount() - 1);
						return ActionResultType.CONSUME;
						
					}
					
				}
				
			}
			
		}
		
		return ActionResultType.PASS;
		
	}
	
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		
		if (!entityIn.isImmuneToFire() && state.get(LIT) && entityIn instanceof LivingEntity && !EnchantmentHelper.hasFrostWalker((LivingEntity)entityIn)) {
			
			entityIn.attackEntityFrom(DamageSource.IN_FIRE, 0.5F);
			
		}
		
		super.onEntityCollision(state, worldIn, pos, entityIn);
		
	}
	
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		
		IWorld iworld = context.getWorld();
		BlockPos blockpos = context.getPos();
		boolean flag = iworld.getFluidState(blockpos).getFluid() == Fluids.WATER;
		
		return this.getDefaultState().with(WATERLOGGED, Boolean.valueOf(flag)).with(FACING, Direction.NORTH).with(FACING, context.getPlacementHorizontalFacing());
		
	}
	
	@SuppressWarnings("deprecation")
	public int getLightValue(BlockState state) {
		
		return state.get(LIT) ? super.getLightValue(state) : 0;
		
	}
	
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		
		return VoxelShapes.or(VoxelShapes.or(VoxelShapes.or(Block.makeCuboidShape(1.0D, 0.0D, 1.0D, 5.0D, 8.0D, 5.0D), Block.makeCuboidShape(15.0D, 0.0D, 1.0D, 11.0D, 8.0D, 5.0D)), Block.makeCuboidShape(1.0D, 0.0D, 15.0D, 5.0D, 8.0D, 11.0D), Block.makeCuboidShape(15.0D, 0.0D, 15.0D, 11.0D, 8.0D, 11.0D)), Block.makeCuboidShape(1.0D, 8.0D, 1.0D, 15.0D, 15.0D, 15.0D));
	
	}
	
	@OnlyIn(Dist.CLIENT)
	public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
		
		if (stateIn.get(LIT)) {
			
			if (rand.nextInt(10) == 0) {
				
				worldIn.playSound((double)((float)pos.getX() + 0.5F), (double)((float)pos.getY() + 0.5F), (double)((float)pos.getZ() + 0.5F), SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.6F, false);
				
			}
			
			if (rand.nextInt(12) == 0) {
				
				for (int i = 0; i < rand.nextInt(1) + 1; ++i) {
					
					worldIn.addParticle(ParticleTypes.LAVA, (double)((float)pos.getX() + 0.5F), (double)((float)pos.getY() + 0.5F), (double)((float)pos.getZ() + 0.5F), (double)(rand.nextFloat() / 2.0F), 5.0E-5D, (double)(rand.nextFloat() / 2.0F));
					
				}
				
			}
			
			if (rand.nextBoolean()) {
				
				worldIn.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, (double)pos.getX() + 0.5D + worldIn.getRandom().nextDouble() / 3.0D * (double)(worldIn.getRandom().nextBoolean() ? 1 : -1), (double)pos.getY() + worldIn.getRandom().nextDouble() + worldIn.getRandom().nextDouble() + 0.4F, (double)pos.getZ() + 0.5D + worldIn.getRandom().nextDouble() / 3.0D * (double)(worldIn.getRandom().nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
				
			}
			
		}
		
	}
	
	public boolean receiveFluid(IWorld worldIn, BlockPos pos, BlockState state, IFluidState fluidStateIn) {
		
		if (!state.get(BlockStateProperties.WATERLOGGED) && fluidStateIn.getFluid() == Fluids.WATER) {
			
			boolean flag = state.get(LIT);
			
			if (flag) {
				
				if (worldIn.isRemote()) {
					
					for (int i = 0; i < 15; ++i) {
						
						worldIn.getWorld().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, (double)pos.getX() + 0.5D + worldIn.getRandom().nextDouble() / 3.0D * (double)(worldIn.getRandom().nextBoolean() ? 1 : -1), (double)pos.getY() + worldIn.getRandom().nextDouble() + worldIn.getRandom().nextDouble(), (double)pos.getZ() + 0.5D + worldIn.getRandom().nextDouble() / 3.0D * (double)(worldIn.getRandom().nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
						
					}
					
				} else {
					
					worldIn.playSound((PlayerEntity)null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
					
				}
				
	         }
			
			worldIn.setBlockState(pos, state.with(WATERLOGGED, Boolean.valueOf(true)).with(LIT, Boolean.valueOf(false)), 3);
			worldIn.getPendingFluidTicks().scheduleTick(pos, fluidStateIn.getFluid(), fluidStateIn.getFluid().getTickRate(worldIn));
			
			return true;
			
		} else {
			
			return false;
			
		}
		
	}

	
	@Nullable
	private Entity getShootingEntity(Entity entity) {
		
		if (entity instanceof AbstractFireballEntity) {
			
			return ((AbstractFireballEntity)entity).shootingEntity;
			
		} else {
			
			return entity instanceof AbstractArrowEntity ? ((AbstractArrowEntity)entity).getShooter() : null;
			
		}
		
	}
	
	public void onProjectileCollision(World worldIn, BlockState state, BlockRayTraceResult hit, Entity projectile) {
		
		if (!worldIn.isRemote) {
			
			boolean flag = projectile instanceof AbstractFireballEntity || projectile instanceof AbstractArrowEntity && projectile.isBurning();
			
			if (flag) {
				
				Entity entity = this.getShootingEntity(projectile);
				
				boolean flag1 = entity == null || entity instanceof PlayerEntity || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(worldIn, entity);
				
				if (flag1 && !state.get(LIT) && !state.get(WATERLOGGED)) {
					
					BlockPos blockpos = hit.getPos();
					worldIn.setBlockState(blockpos, state.with(BlockStateProperties.LIT, Boolean.valueOf(true)), 11);
					
				}
				
			}
			
		}
		
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
		
		builder.add(WATERLOGGED, FACING, LIT);
		
	}
	
	public boolean allowsMovement(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
		
		return false;
		
	}
	
}