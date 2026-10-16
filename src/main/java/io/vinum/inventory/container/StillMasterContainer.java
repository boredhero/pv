package io.vinum.inventory.container;

import io.vinum.tileentity.StillMasterTileEntity;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StillMasterContainer extends AbstractContainerMenu {
    private static final int PLAYER_START = 4;
    private static final int HOTBAR_START = 31;
    private static final int PLAYER_END = 40;
    private final Container inventory;
    private final ContainerData stillData;

    public StillMasterContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(4), new SimpleContainerData(3));
    }

    public StillMasterContainer(int id, Inventory playerInventory, Container inventory, ContainerData stillData) {
        super(ModContainers.STILL_MASTER.get(), id);
        checkContainerSize(inventory, 4);
        checkContainerDataCount(stillData, 3);
        this.inventory = inventory;
        this.stillData = stillData;
        inventory.startOpen(playerInventory.player);
        addInputSlot(0, 43, 17);
        addInputSlot(1, 117, 17);
        addInputSlot(2, 43, 53);
        addSlot(new Slot(inventory, 3, 117, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(stillData);
    }

    private void addInputSlot(int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return StillMasterTileEntity.isValidInput(index, stack);
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int machineSlot = -1;
            for (int candidate = 0; candidate < 3; candidate++) {
                if (StillMasterTileEntity.isValidInput(candidate, stack)) {
                    machineSlot = candidate;
                    break;
                }
            }
            if (machineSlot >= 0) {
                if (!moveItemStackTo(stack, machineSlot, machineSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    public int getCookProgressionScaled() {
        return Math.clamp(stillData.get(0) * 24 / StillMasterTileEntity.PROCESSING_TICKS, 0, 24);
    }

    public int getBurnLeftScaled() {
        int duration = stillData.get(2);
        return duration <= 0 ? 0 : Math.clamp(stillData.get(1) * 13 / duration, 0, 13);
    }
}
