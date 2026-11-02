package io.vinum.machine;

import io.vinum.inventory.container.ModContainers;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

public final class WinePressMenu extends AbstractContainerMenu {
  private final WinePressBlockEntity press;
  private final ContainerData data;

  public WinePressMenu(int id, Inventory inventory) {
    this(id, inventory, null, new ItemStackHandler(3), new SimpleContainerData(7));
  }

  public WinePressMenu(int id, Inventory inventory, WinePressBlockEntity press) {
    this(id, inventory, press, press.inventory, press.data);
  }

  private WinePressMenu(
      int id,
      Inventory inventory,
      WinePressBlockEntity press,
      IItemHandler items,
      ContainerData data) {
    super(ModContainers.WINE_PRESS.get(), id);
    this.press = press;
    this.data = data;
    addSlot(new SlotItemHandler(items, 0, 35, 35));
    addSlot(new SlotItemHandler(items, 1, 112, 23));
    addSlot(
        new SlotItemHandler(items, 2, 112, 65) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        });
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, 126 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 184));
    addDataSlots(data);
  }

  public int energy() {
    return data.get(0);
  }

  public int progress() {
    return data.get(1);
  }

  public int required() {
    return data.get(2);
  }

  public int juice() {
    return data.get(3);
  }

  public boolean hasCrank() {
    return (data.get(4) & 1) != 0;
  }

  public boolean hasMotor() {
    return (data.get(4) & 2) != 0;
  }

  public boolean clarifying() {
    return (data.get(4) & 4) != 0;
  }

  public int color() {
    return (data.get(5) & 65535) | ((data.get(6) & 65535) << 16);
  }

  @Override
  public boolean stillValid(Player player) {
    return press == null || press.usable(player);
  }

  @Override
  public boolean clickMenuButton(Player player, int button) {
    return press != null
        && press.usable(player)
        && (button == 0 ? press.turnCrank() : button == 1 && press.clarifyAdjacentVat());
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    Slot slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    ItemStack stack = slot.getItem(), original = stack.copy();
    if (index < 3) {
      if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
    } else if (slots.get(0).mayPlace(stack)) {
      if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    } else if (slots.get(1).mayPlace(stack)) {
      if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
    } else if (index < 30) {
      if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
    slot.onTake(player, stack);
    return original;
  }
}
