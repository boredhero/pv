/*
	Project Vinum - StillMasterTileEntity.java
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
package io.vinum.tileentity;

import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeHooks;
import io.vinum.ProjectVinum;
import io.vinum.block.PVBlocks;
import io.vinum.block.state.properties.PVBlockStateProperties;
import io.vinum.common.PVDefines;
import io.vinum.inventory.container.PVContainers;
import io.vinum.inventory.container.StillMasterContainer;
import io.vinum.tileentity.recipes.StillRecipes;

public class StillMasterTileEntity extends LockableTileEntity implements ITickableTileEntity {
	
	protected NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
	
	public StillRecipes.StillRecipe currentRecipe;
	
	public int fuelTime;
	public int totalFuelTime;
	public int progressTime;
	public int totalProgressTime;
	
	public int currentPressure;
	
	public int animatePressureTime;
	
	boolean hasMaxPressure;
	
	protected final IIntArray stillData = new IIntArray() {
		
		public int get(int index) {
			
			switch(index) {
			
			case 0:
				return fuelTime;
				
			case 1:
				return totalFuelTime;
				
			case 2:
				return progressTime;
				
			case 3:
				return totalProgressTime;
				
			default:
				return 0;
				
			}
			
		}
		
		public void set(int index, int value) {
			
			switch(index) {
			
			case 0:
				fuelTime = value;
				break;
				
			 case 1:
				totalFuelTime = value;
				break;
				
			 case 2:
				progressTime = value;
				break;
				
			 case 3:
				totalProgressTime = value;
				break;
				
			}
			
		}
		
		public int size() {
			
			return 4;
			
		}
		
	};
	
	public StillMasterTileEntity() {
		super(PVTileEntities.STILL_MASTER.get());
		
	}
	
	public void read(CompoundNBT compound) {
		super.read(compound);
		
		this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(compound, this.items);
		this.fuelTime = compound.getInt("FuelTime");
		this.progressTime = compound.getInt("ProgressTime");
		this.totalProgressTime = compound.getInt("TotalProgressTime");
		this.currentPressure = compound.getInt("CurrentPressure");
		
	}
	
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		
		ItemStackHelper.saveAllItems(compound, this.items);
		compound.putInt("FuelTime", this.fuelTime);
		compound.putInt("ProgressTime", this.progressTime);
		compound.putInt("TotalProgressTime", this.totalProgressTime);
		compound.putInt("CurrentPressure", this.currentPressure);
		
		return compound;
		
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		
		this.read(pkt.getNbtCompound());
		world.notifyBlockUpdate(pos, world.getBlockState(pos).getBlock().getDefaultState(), world.getBlockState(pos), 2);
		
	}

	@Override
	public CompoundNBT getUpdateTag() {
		
		CompoundNBT compound = new CompoundNBT();
		
		this.write(compound);
		return compound;
		
	}
	
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		
		return new SUpdateTileEntityPacket(this.pos, 1, this.getUpdateTag());
		
	}
	
	@Override
	public void tick() {
		
		boolean flag = false;
		
		if (!world.isRemote()) {
			
			ItemStack input = this.items.get(0);
			ItemStack bottle = this.items.get(1);
			ItemStack fuel = this.items.get(2);
			ItemStack output = this.items.get(3);
			
			this.currentRecipe = null;
			
			for (StillRecipes.StillRecipe recipe : ProjectVinum.STILL_RECIPES.getRecipes()) {
				
				if (recipe.matchesRecipe(input, bottle)) {
					
					flag = true;
					this.currentRecipe = recipe;
					
				}
				
			}
			
			//System.out.println(progressTime + " | " + totalProgressTime + " | " + fuelTime + " | " + canStill());
			
			if (this.currentRecipe != null) {
				
				this.totalProgressTime = this.currentRecipe.getTicksToDistill();
				
				if (fuel != null && fuelTime <= 0 && (progressTime != 0 || canStill())) {
					
					if (AbstractFurnaceTileEntity.isFuel(fuel)) {
						
						flag = true;
						fuelTime = ForgeHooks.getBurnTime(fuel);
						totalFuelTime = ForgeHooks.getBurnTime(fuel);
						fuel.shrink(1);
						
					} else {
						
						totalFuelTime = 0;
						
					}
					
				}
				
				if (fuelTime > 0) {
					
					flag = true;
					fuelTime--;
					
					if (canStill()) {
						
						progressTime++;
						
					}
					
				}
				
				if (this.currentRecipe.getDistilledItemstack().getItem() != input.getItem()) {
					
					progressTime = 0;
					
				}
				
				if (progressTime >= this.totalProgressTime) {
					
					flag = true;
					
					input.shrink(1);
					bottle.shrink(1);
					
					if (output.getItem() != this.currentRecipe.getDistillingResultItemstack().getItem()) {
						
						this.items.set(3, new ItemStack(this.currentRecipe.getDistillingResultItemstack().getItem(), 1));
						
					} else if (output.getItem() == this.currentRecipe.getDistillingResultItemstack().getItem()) {
						
						output.grow(1);
						
					} else {
						
						this.items.set(3, new ItemStack(this.currentRecipe.getDistillingResultItemstack().getItem(), 1));
						
					}
					
					progressTime = 0;
					
				}
				
			} else {
				
				this.totalProgressTime = 0;
				
			}
			
			if (totalProgressTime > 0 && (fuelTime > 0 || AbstractFurnaceTileEntity.isFuel(fuel))) {
				
				if (progressTime <= totalProgressTime) {
					
					currentPressure = 3;
					
				}
				
				if (progressTime <= (totalProgressTime / 4) * 3 && !hasMaxPressure) {
					
					currentPressure = 2;
					
				}
				
				if (progressTime <= (totalProgressTime / 4) * 2 && !hasMaxPressure) {
					
					currentPressure = 1;
					
				}
				
				if (progressTime <= totalProgressTime / 4 && !hasMaxPressure) {
					
					currentPressure = 0;
					
				}
				
				if (currentPressure == 3) {
					
					hasMaxPressure = true;
					
				}
				
				animatePressureTime = 10;
				
			} else if (currentPressure > 0) {
				
				if (animatePressureTime > 0) {
					
					animatePressureTime--;
					
				} else {
					
					animatePressureTime = 10;
					currentPressure--;
					
				}
				
				hasMaxPressure = false;
				
			}
			
			if (world.getBlockState(pos.up()).getBlock() == PVBlocks.STILL_MULTIBLOCK_PART_2.get()) {
				
				if (world.getBlockState(pos.up()).get(PVBlockStateProperties.PRESSURE) != currentPressure) {
					
					flag = true;
					this.world.setBlockState(this.pos.up(), this.world.getBlockState(this.pos.up()).with(PVBlockStateProperties.PRESSURE, currentPressure), 3);
					
				}
				
				
			}
			
			if (fuelTime > 0) {
				
				flag = true;
				this.world.setBlockState(this.pos, this.world.getBlockState(this.pos).with(BlockStateProperties.LIT, Boolean.TRUE), 3);
				
			} else if (fuelTime <= 0 && this.getBlockState().get(BlockStateProperties.LIT) == true) {
				
				flag = true;
				this.world.setBlockState(pos, world.getBlockState(this.pos).with(BlockStateProperties.LIT, Boolean.FALSE), 3);
				
			}
			
		}
		
		if (flag) {
			
			this.markDirty();
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	public boolean canStill() {
		
		if (this.currentRecipe != null) {
			
			if (this.items.get(3).getItem() == Item.getItemFromBlock(Blocks.AIR) || this.items.get(3).getItem() == this.currentRecipe.getDistillingResultItemstack().getItem()) {
				
				return (this.items.get(3).getCount() <= 63 && this.items.get(0).getItem() == this.currentRecipe.getDistilledItemstack().getItem() && this.items.get(1).getItem() == this.currentRecipe.getRequiredBottleItemstack().getItem());
				
			}
			
		}
		
		return false;
		
	}
	
	@Override
	public int getSizeInventory() {
		
		return items.size();
		
	}
	
	@Override
	public boolean isEmpty() {
		
		for(ItemStack itemstack : this.items) {
			
			if (!itemstack.isEmpty()) {
				
				return false;
				
			}
			
		}
		
		return true;
		  
	}
	
	@Override
	public ItemStack getStackInSlot(int index) {
		
		return this.items.get(index);
		
	}
	
	@Override
	public ItemStack decrStackSize(int index, int count) {
		
		return ItemStackHelper.getAndSplit(this.items, index, count);
		
	}
	
	@Override
	public ItemStack removeStackFromSlot(int index) {
		
		return ItemStackHelper.getAndRemove(this.items, index);
		
	}
	
	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		
		this.items.set(index, stack);
		
	}
	
	@Override
	public boolean isUsableByPlayer(PlayerEntity player) {
		
		if (this.world.getTileEntity(this.pos) != this) {
			
			return false;
			
		} else {
			
			return player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) <= 64.0D;
			
		}
		
	}
	
	@Override
	public void clear() {
		
		this.items.clear();
		
	}
	
	@Override
	protected ITextComponent getDefaultName() {
		
		return new TranslationTextComponent(PVDefines.MODID + ":container.still");
		
	}
	
	@Override
	protected Container createMenu(int id, PlayerInventory player) {
		
		return new StillMasterContainer(id, this, player, this.stillData);
		
	}
	
}