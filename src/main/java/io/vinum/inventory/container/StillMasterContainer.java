/*
    Project Vinum - StillMasterContainer.java
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class StillMasterContainer extends Container {
	
	private IIntArray stillData;
	
	public StillMasterContainer(int id, PlayerInventory playerInventory, PacketBuffer extraData) {
		this(id, new Inventory(4), playerInventory, new IntArray(4));
	  
	}
	
	public StillMasterContainer(int id, IInventory stillInventory, PlayerInventory playerInventory, IIntArray stillData) {
		super(PVContainers.STILL_MASTER.get(), id);
		
		assertInventorySize(stillInventory, 4);
		assertIntArraySize(stillData, 4);
		
		this.stillData = stillData;
		
		this.addSlot(new Slot(stillInventory, 0, 43, 17));
		this.addSlot(new Slot(stillInventory, 1, 117, 17));
		this.addSlot(new Slot(stillInventory, 2, 43, 53));
		this.addSlot(new FurnaceResultSlot(playerInventory.player, stillInventory, 3, 117, 53));
		
		for (int i = 0; i < 3; i++) {
			
			for (int j = 0; j < 9; j++) {
				
				this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
				
			}
			
		}
		
		for (int k = 0; k < 9; ++k) {
			
			this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
			
		}
		
		this.trackIntArray(stillData);
		
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
		
		int i = stillData.get(2);
		int j = stillData.get(3);
		
		if (i == 0 || j == 0) {
			
			return 0;
			
		}
		
		return j != 0 && i != 0 ? i * 56 / j : 0;
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public int getBurnLeftScaled() {
		
		if (stillData.get(0) == 0 || stillData.get(1) == 0) {
			
			return 0;
			
		}
		
		return stillData.get(0) * 13 / stillData.get(1);
		
	}
	
}