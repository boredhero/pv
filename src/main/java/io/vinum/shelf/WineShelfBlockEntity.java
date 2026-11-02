package io.vinum.shelf;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.ModDataComponents;
import io.vinum.beverage.WineCatalog;
import io.vinum.beverage.BeverageAppearance;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Five independent bottle positions; full item components survive storage and transfers. */
public final class WineShelfBlockEntity extends BlockEntity {
  public static final int SLOTS = 5;
  private NonNullList<ItemStack> bottles = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
  private int agingTicks;
  public WineShelfBlockEntity(BlockPos pos, BlockState state) { super(ShelfContent.ENTITY.get(), pos, state); }
  public static boolean accepts(ItemStack stack) {
    return stack.is(ModItems.WINE_BOTTLE_EMPTY.get()) || stack.is(ModItems.FIFTH_BOTTLE_EMPTY.get());
  }
  public ItemStack bottle(int slot) { return slot >= 0 && slot < SLOTS ? bottles.get(slot).copy() : ItemStack.EMPTY; }
  public boolean insert(int slot, ItemStack stack) {
    if (slot < 0 || slot >= SLOTS || !bottles.get(slot).isEmpty() || !accepts(stack)) return false;
    bottles.set(slot, stack.copyWithCount(1)); changed(); return true;
  }
  public ItemStack remove(int slot) {
    if (slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
    ItemStack stack = bottles.get(slot); bottles.set(slot, ItemStack.EMPTY);
    if (!stack.isEmpty()) changed();
    return stack;
  }
  private void changed() {
    setChanged();
    if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
  }
  public static void tick(Level level, BlockPos pos, BlockState state, WineShelfBlockEntity shelf) {
    if (level.isClientSide || ++shelf.agingTicks < 20) return;
    shelf.agingTicks = 0;
    boolean changed = false;
    boolean appearanceChanged = false;
    for (ItemStack bottle : shelf.bottles) {
      if (!(bottle.getItem() instanceof FluidContainerItem container)) continue;
      var fluid = container.contents(bottle);
      if (!WineCatalog.isWine(fluid)) continue; // Glass storage must not create barrel-aged tequila.
      var previousAppearance = BeverageAppearance.find(fluid);
      long age = BarrelFluidData.getAge(fluid);
      bottle.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(BarrelFluidData.withAge(fluid, Math.min(Long.MAX_VALUE - 20, age) + 20)));
      bottle.remove(ModDataComponents.FLUID_AGE.get());
      appearanceChanged |= !previousAppearance.equals(BeverageAppearance.find(container.contents(bottle)));
      changed = true;
    }
    if (appearanceChanged) shelf.changed();
    else if (changed) shelf.setChanged();
  }
  @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, bottles, registries); tag.putInt("AgingTicks", agingTicks);
  }
  @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries); bottles = NonNullList.withSize(SLOTS, ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, bottles, registries);
    agingTicks = Math.clamp(tag.getInt("AgingTicks"), 0, 19);
  }
  @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
  @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
