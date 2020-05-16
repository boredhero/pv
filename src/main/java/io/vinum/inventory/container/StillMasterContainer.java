package io.vinum.inventory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.FurnaceFuelSlot;
import net.minecraft.inventory.container.FurnaceResultSlot;
import net.minecraft.inventory.container.Slot;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.registries.ObjectHolder;

public class StillMasterContainer extends Container {
	
	public StillMasterContainer(int id, PlayerInventory playerInv, PacketBuffer extraData) {
        this(ModContainers.STILL_MASTER.get(), id, playerInv);
        
    }
	
	public StillMasterContainer(ContainerType<?> type, int id, PlayerInventory playerInv) {
		this(type, id, new Inventory(4), playerInv);
		
	}
	
	public StillMasterContainer(ContainerType<?> type, int id, IInventory inventory, PlayerInventory playerInv) {
		super(type, id);
		
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
	
}