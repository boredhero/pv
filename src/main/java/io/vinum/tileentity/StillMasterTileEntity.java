package io.vinum.tileentity;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import io.vinum.block.SteelComponentBlock;
import io.vinum.common.Defines;
import io.vinum.inventory.container.StillMasterContainer;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** The assembled still has one inventory and one server-side processing clock. */
public class StillMasterTileEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int WORT_SLOT = 0;
    public static final int BOTTLE_SLOT = 1;
    public static final int FUEL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    public static final int PROCESSING_TICKS = 240;
    private static final int[] TOP_SLOTS = {WORT_SLOT, BOTTLE_SLOT};
    private static final int[] SIDE_SLOTS = {FUEL_SLOT};
    private static final int[] BOTTOM_SLOTS = {OUTPUT_SLOT, FUEL_SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ItemStack resultTemplate = new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get());
    private final IItemHandler unsidedHandler = new InvWrapper(this);
    private final Map<Direction, IItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    public int fuelTime;
    public int progressTime;
    public int fuelDuration;

    private final ContainerData stillData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progressTime;
                case 1 -> fuelTime;
                case 2 -> fuelDuration;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progressTime = value;
                case 1 -> fuelTime = value;
                case 2 -> fuelDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public StillMasterTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.STILL_MASTER.get(), pos, state);
    }

    public ContainerData getStillData() {
        return stillData;
    }

    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == null ? unsidedHandler
                : sidedHandlers.computeIfAbsent(side, direction -> new SidedInvWrapper(this, direction));
    }

    public boolean isLit() {
        return fuelTime > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StillMasterTileEntity still) {
        if (level.isClientSide) {
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            Direction facing = state.getValue(SteelComponentBlock.FACING);
            if (StillMultiblock.isLoaded(level, pos, facing)
                    && !StillMultiblock.isComplete(level, pos, facing)) {
                level.scheduleTick(pos, state.getBlock(), 1);
                return;
            }
        }
        if (!still.canStill()) {
            if (still.progressTime != 0) {
                still.progressTime = 0;
                still.setChanged();
            }
            return;
        }
        if (still.fuelTime <= 0) {
            ItemStack fuel = still.items.get(FUEL_SLOT);
            int burnTime = fuel.getBurnTime(RecipeType.SMELTING);
            if (burnTime <= 0) {
                if (still.progressTime != 0) {
                    still.progressTime = 0;
                    still.setChanged();
                }
                return;
            }
            ItemStack remainder = fuel.getCraftingRemainingItem();
            fuel.shrink(1);
            if (!remainder.isEmpty()) {
                if (fuel.isEmpty()) {
                    still.items.set(FUEL_SLOT, remainder);
                } else {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, remainder);
                }
            }
            still.fuelTime = burnTime;
            still.fuelDuration = burnTime;
        }
        // Consume exactly one unit of heat per active processing tick; no zero-fuel progress.
        still.fuelTime--;
        if (++still.progressTime >= PROCESSING_TICKS) {
            still.items.get(WORT_SLOT).shrink(1);
            still.items.get(BOTTLE_SLOT).shrink(1);
            ItemStack output = still.items.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                still.items.set(OUTPUT_SLOT, still.resultTemplate.copy());
            } else {
                output.grow(1);
            }
            still.progressTime = 0;
        }
        still.setChanged();
    }

    public boolean canStill() {
        if (!items.get(WORT_SLOT).is(ModItems.FERMENTED_AGAVE_WORT.get())
                || !items.get(BOTTLE_SLOT).is(ModItems.FIFTH_BOTTLE_EMPTY.get())) {
            return false;
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, resultTemplate)
                && output.getCount() < getMaxStackSize(output);
    }

    public static boolean isValidInput(int slot, ItemStack stack) {
        return switch (slot) {
            case WORT_SLOT -> stack.is(ModItems.FERMENTED_AGAVE_WORT.get());
            case BOTTLE_SLOT -> stack.is(ModItems.FIFTH_BOTTLE_EMPTY.get());
            case FUEL_SLOT -> stack.getBurnTime(RecipeType.SMELTING) > 0;
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidInput(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOTS : side == Direction.UP ? TOP_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT || slot == FUEL_SLOT && !isValidInput(FUEL_SLOT, stack);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void clearContent() {
        super.clearContent();
        progressTime = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        fuelTime = Math.max(0, tag.getInt("FuelTime"));
        progressTime = Math.clamp(tag.getInt("ProgressTime"), 0, PROCESSING_TICKS - 1);
        fuelDuration = Math.max(fuelTime, tag.getInt("FuelDuration"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("FuelTime", fuelTime);
        tag.putInt("ProgressTime", progressTime);
        tag.putInt("FuelDuration", fuelDuration);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(Defines.MODID + ":container.still");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new StillMasterContainer(id, inventory, this, stillData);
    }
}
