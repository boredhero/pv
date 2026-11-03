package io.vinum.bar;

import io.vinum.item.FluidContainerItem;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Display storage owns complete item stacks; it never drains, blends or ages their contents. */
public final class BarBlockEntity extends BlockEntity {
    public static final int SLOTS = 5;
    public static final TagKey<Item> BOTTLES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("vinum", "bar_bottles"));
    private NonNullList<ItemStack> bottles = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    public BarBlockEntity(BlockPos pos, BlockState state) { super(BarContent.ENTITY.get(), pos, state); }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null && !level.isClientSide) level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
    }
    public static boolean accepts(ItemStack stack) { return !stack.isEmpty() && stack.is(BOTTLES); }
    public ItemStack bottle(int slot) { return slot >= 0 && slot < SLOTS ? bottles.get(slot).copy() : ItemStack.EMPTY; }
    public boolean insert(int slot, ItemStack stack) {
        if (level == null || level.isClientSide || slot < 0 || slot >= SLOTS || !bottles.get(slot).isEmpty() || !accepts(stack)) return false;
        bottles.set(slot, stack.copyWithCount(1)); changed(); return true;
    }
    /** Commit a serving/drinking result only if the displayed stack still matches the snapshot. */
    public boolean replace(int slot, ItemStack expected, ItemStack replacement) {
        if(level==null || level.isClientSide || slot<0 || slot>=SLOTS
            || !ItemStack.isSameItemSameComponents(bottles.get(slot),expected)) return false;
        bottles.set(slot,replacement.copyWithCount(1));changed();return true;
    }
    public ItemStack remove(int slot) {
        if (level == null || level.isClientSide || slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
        ItemStack result = bottles.get(slot); bottles.set(slot, ItemStack.EMPTY);
        if (!result.isEmpty()) changed();
        return result;
    }
    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, bottles, registries);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); bottles = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, bottles, registries);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
