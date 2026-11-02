package io.vinum.machine;

import io.vinum.inventory.container.ModContainers;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

public final class BasicGeneratorMenu extends AbstractContainerMenu {
  private final BasicGeneratorBlockEntity generator;
  private final ContainerData data;

  public BasicGeneratorMenu(int id, Inventory player) {
    this(id, player, null, new ItemStackHandler(2), new SimpleContainerData(5));
  }

  public BasicGeneratorMenu(int id, Inventory player, BasicGeneratorBlockEntity generator) {
    this(id, player, generator, generator.inventory, generator.data);
  }

  private BasicGeneratorMenu(
      int id,
      Inventory player,
      BasicGeneratorBlockEntity generator,
      IItemHandler items,
      ContainerData data) {
    super(ModContainers.BASIC_GENERATOR.get(), id);
    this.generator = generator;
    this.data = data;
    addSlot(new SlotItemHandler(items, 0, 44, 35));
    addSlot(
        new SlotItemHandler(items, 1, 116, 35) {
          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        });
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
    addDataSlots(data);
  }

  public int energy() {
    return data.get(0);
  }

  public int burnRemaining() {
    return (data.get(1) & 65535) | ((data.get(2) & 65535) << 16);
  }

  public int burnDuration() {
    return (data.get(3) & 65535) | ((data.get(4) & 65535) << 16);
  }

  @Override
  public boolean stillValid(Player player) {
    return generator == null || generator.usable(player);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    Slot slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    ItemStack stack = slot.getItem(), original = stack.copy();
    if (index < 2) {
      if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
    } else if (slots.get(0).mayPlace(stack)) {
      if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
    } else if (index < 29) {
      if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
    slot.onTake(player, stack);
    return original;
  }
}
