package io.vinum.machine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Slot projection with explicit insertion/extraction roles for automation. */
public final class SidedItemView implements IItemHandler {
  private final IItemHandler delegate;
  private final int[] slots;
  private final boolean insert, extract;

  public SidedItemView(IItemHandler delegate, boolean insert, boolean extract, int... slots) {
    this.delegate = delegate;
    this.slots = slots.clone();
    this.insert = insert;
    this.extract = extract;
  }

  @Override
  public int getSlots() {
    return slots.length;
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    return delegate.getStackInSlot(slots[slot]);
  }

  @Override
  public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
    return insert ? delegate.insertItem(slots[slot], stack, simulate) : stack;
  }

  @Override
  public ItemStack extractItem(int slot, int amount, boolean simulate) {
    return extract ? delegate.extractItem(slots[slot], amount, simulate) : ItemStack.EMPTY;
  }

  @Override
  public int getSlotLimit(int slot) {
    return delegate.getSlotLimit(slots[slot]);
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    return insert && delegate.isItemValid(slots[slot], stack);
  }
}
