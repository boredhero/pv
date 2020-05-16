package io.vinum.tileentity;

import io.vinum.common.Defines;
import io.vinum.inventory.container.ModContainers;
import io.vinum.inventory.container.StillMasterContainer;
import io.vinum.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.FurnaceContainer;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ObjectHolder;

public class StillMasterTileEntity extends LockableTileEntity implements ITickableTileEntity {
	
	protected NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
	
	private int fuelTime;
	private int progressTime;
	
	protected final IIntArray stillData = new IIntArray() {
	      public int get(int index) {
	         switch(index) {
	         case 0:
	            return progressTime;
	         case 1:
	            return fuelTime;
	         default:
	            return 0;
	         }
	      }

	      public void set(int index, int value) {
	         switch(index) {
	         case 0:
	            progressTime = value;
	            break;
	         case 1:
	            fuelTime = value;
	            break;
	         }

	      }

	      public int size() {
	         return 2;
	      }
	   };
	
	public StillMasterTileEntity() {
		super(ModTileEntities.STILL_MASTER.get());
		
	}
	
	public void read(CompoundNBT compound) {
		super.read(compound);
		
		this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(compound, this.items);
		this.fuelTime = compound.getInt("FuelTime");
		this.progressTime = compound.getInt("ProgressTime");
		
	}
	
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		
		ItemStackHelper.saveAllItems(compound, this.items);
		compound.putInt("FuelTime", this.fuelTime);
		compound.putInt("ProgressTime", this.progressTime);
		
		return compound;
		
	}
	
	@Override
	public void tick() {
		
		BlockState state = world.getBlockState(pos);
		
		if (!world.isRemote()) {
			
			ItemStack input = this.items.get(0);
			ItemStack bottle = this.items.get(1);
			ItemStack fuel = this.items.get(2);
			ItemStack output = this.items.get(3);
			
			if (fuel != null && fuelTime <= 0 && (progressTime != 0 || canStill())) {
				
				if (AbstractFurnaceTileEntity.isFuel(fuel)) {
					
					fuelTime = AbstractFurnaceTileEntity.getBurnTimes().get(fuel);
					fuel.shrink(1);
					
				}
				
			}
			
			if (fuelTime >= 0 && canStill()) {
				
				progressTime++;
				
			} else {
				
				progressTime = 0;
				
			}
			
			if (progressTime >= 240) {
				
				input.shrink(1);
				bottle.shrink(1);
				
				if (output == null) {
					
					output = new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
					
				} else if (output.getItem() == ModItems.FIFTH_SILVER_TEQUILA.get()) {
					
					output.grow(1);
					
				}
				
			}
			
		}
		
		if (state != world.getBlockState(pos)) {
			
			this.markDirty();
			
		}
		
	}
	
	public boolean canStill() {
		
		return (this.items.get(0).getItem() == ModItems.FERMENTED_AGAVE_WORT.get() && this.items.get(1).getItem() == ModItems.FIFTH_BOTTLE_EMPTY.get() && (this.items.get(3).getItem() == ModItems.FIFTH_SILVER_TEQUILA.get()) || this.items.get(3) == null);
		
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
		
		return new TranslationTextComponent(Defines.MODID + ":container.still");
		
	}
	
	@Override
	protected Container createMenu(int id, PlayerInventory player) {
		
		return new StillMasterContainer(ModContainers.STILL_MASTER.get(), id, this, player);
		
	}
	
}