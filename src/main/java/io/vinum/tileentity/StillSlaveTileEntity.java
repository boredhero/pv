package io.vinum.tileentity;

import io.vinum.common.Defines;
import io.vinum.inventory.container.ModContainers;
import io.vinum.inventory.container.StillMasterContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class StillSlaveTileEntity extends LockableTileEntity implements ITickableTileEntity {
	
	protected NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
	
	private StillMasterTileEntity masterTileEntity;
	
	private BlockPos masterTileEntityPos;
	
	public StillSlaveTileEntity() {
		super(ModTileEntities.STILL_SLAVE.get());
		
	}
	
	public void read(CompoundNBT compound) {
		super.read(compound);
		
		this.items = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
		ItemStackHelper.loadAllItems(compound, this.items);
		
		masterTileEntityPos = new BlockPos(compound.getInt("MasterTileEntityX"), compound.getInt("MasterTileEntityY"), compound.getInt("MasterTileEntityZ"));
		
	}
	
	public CompoundNBT write(CompoundNBT compound) {
		super.write(compound);
		
		ItemStackHelper.saveAllItems(compound, this.items);
		
		if (this.masterTileEntity != null) {
			
			compound.putInt("MasterTileEntityX", this.masterTileEntity.getPos().getX());
			compound.putInt("MasterTileEntityY", this.masterTileEntity.getPos().getY());
			compound.putInt("MasterTileEntityZ", this.masterTileEntity.getPos().getZ());
			
		}
		
		return compound;
		
	}
	
	@Override
	public void tick() {
		
		if (this.masterTileEntity == null) {
			
			if (this.hasWorld() && !this.getWorld().isRemote()) {
				
				if (this.getWorld().getTileEntity(masterTileEntityPos) instanceof StillMasterTileEntity) {
					
					this.masterTileEntity = (StillMasterTileEntity) this.getWorld().getTileEntity(masterTileEntityPos);
					
				}
				
			}
			
		}
		
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
		
		return masterTileEntity.createMenu(id, player);
		
	}
	
	public void setMasterTileEntity(TileEntity tileEntity) {
		
		if (tileEntity instanceof StillMasterTileEntity) {
			
			this.masterTileEntity = (StillMasterTileEntity) tileEntity;
			
		}
		
	}
	
	public StillMasterTileEntity getMasterTileEntity() {
		
		return masterTileEntity;
		
	}
	
}