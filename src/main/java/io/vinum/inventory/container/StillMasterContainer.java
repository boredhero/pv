package io.vinum.inventory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.FurnaceResultSlot;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class StillMasterContainer extends Container {
	
	private IIntArray stillData;
	
	public StillMasterContainer(int id, PlayerInventory playerInv, PacketBuffer extraData) {
		this(ModContainers.STILL_MASTER.get(), id, playerInv, new IntArray(2));
	  
	}
	
	public StillMasterContainer(ContainerType<?> type, int id, PlayerInventory playerInv, IIntArray stillData) {
		this(type, id, new Inventory(4), playerInv, stillData);
		
	}
	
	public StillMasterContainer(ContainerType<?> type, int id, IInventory inventory, PlayerInventory playerInv, IIntArray stillData) {
		super(type, id);
		
		this.stillData = stillData;
		
		this.addSlot(new Slot(inventory, 0, 43, 17));
		this.addSlot(new Slot(inventory, 1, 117, 17));
		this.addSlot(new Slot(inventory, 2, 43, 53));
		this.addSlot(new FurnaceResultSlot(playerInv.player, inventory, 3, 117, 53));
		
		for (int i = 0; i < 3; i++) {
			
			for (int j = 0; j < 9; j++) {
				
				this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
				
			}
			
		}
		
		for (int k = 0; k < 9; ++k) {
			
			this.addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
			
		}
		
	}
	
	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		
		return true;
		
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
	}
	
	public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);
		if (slot != null && slot.getHasStack()) {
		ItemStack itemstack1 = slot.getStack();
		itemstack = itemstack1.copy();
		if (index == 2) {
			if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
				return ItemStack.EMPTY;
			}
			slot.onSlotChange(itemstack1, itemstack);
		} else if (index != 1 && index != 0) {
			if (index >= 3 && index < 30) {
			if (!this.mergeItemStack(itemstack1, 30, 39, false)) {
				return ItemStack.EMPTY;
			}
			} else if (index >= 30 && index < 39 && !this.mergeItemStack(itemstack1, 3, 30, false)) {
			return ItemStack.EMPTY;
			}
		} else if (!this.mergeItemStack(itemstack1, 3, 39, false)) {
			return ItemStack.EMPTY;
		}

		if (itemstack1.isEmpty()) {
			slot.putStack(ItemStack.EMPTY);
		} else {
			slot.onSlotChanged();
		}

		if (itemstack1.getCount() == itemstack.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(playerIn, itemstack1);
		}

		return itemstack;
	}
	
	@OnlyIn(Dist.CLIENT)
	public int getCookProgressionScaled() {
		
		int i = this.stillData.get(1);
		int j = 240;
		return j != 0 && i != 0 ? i * 24 / j : 0;
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public int getBurnLeftScaled() {
		
		return this.stillData.get(0) * 13 / 200;
		
	}
	
}