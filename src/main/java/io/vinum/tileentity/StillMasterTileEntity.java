package io.vinum.tileentity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import io.vinum.production.ProductionContainers;
import io.vinum.production.ProductionRecipes;
import io.vinum.production.DistillationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
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
    private static final int[] BOTTOM_SLOTS = {OUTPUT_SLOT, FUEL_SLOT, WORT_SLOT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ItemStack resultTemplate = new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get());
    private final IItemHandler unsidedHandler = new InvWrapper(this);
    private final Map<Direction, IItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    public static final int WASH_CAPACITY = 16_000;
    private FluidStack wash = FluidStack.EMPTY;
    // A running recipe is persisted so a data-pack reload cannot change a batch halfway through.
    private DistillationRecipe activeRecipe;
    private int clientWashAmount;
    private int clientRequiredAmount;
    private final IFluidHandler washHandler = new IFluidHandler() {
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return wash.copy(); }
        public int getTankCapacity(int tank) { return WASH_CAPACITY; }
        public boolean isFluidValid(int tank, FluidStack fluid) { return acceptsWash(fluid); }
        public int fill(FluidStack fluid, FluidAction action) {
            if (!acceptsWash(fluid) || !wash.isEmpty() && !FluidStack.isSameFluidSameComponents(wash, fluid)) return 0;
            int accepted = Math.min(fluid.getAmount(), WASH_CAPACITY - wash.getAmount());
            if (accepted > 0 && action.execute()) {
                wash = fluid.copyWithAmount(wash.getAmount() + accepted);
                setChanged();
            }
            return accepted;
        }
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            return FluidStack.isSameFluidSameComponents(wash, fluid) ? drain(fluid.getAmount(), action) : FluidStack.EMPTY;
        }
        public FluidStack drain(int amount, FluidAction action) {
            if (amount <= 0 || progressTime > 0 || wash.isEmpty()) return FluidStack.EMPTY;
            FluidStack result = wash.copyWithAmount(Math.min(amount, wash.getAmount()));
            if (action.execute()) { wash.shrink(result.getAmount()); if (wash.isEmpty()) activeRecipe = null; setChanged(); }
            return result;
        }
    };
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
                case 3 -> level != null && !level.isClientSide ? wash.getAmount() : clientWashAmount;
                case 4 -> level != null && !level.isClientSide ? requiredWashAmount() : clientRequiredAmount;
                case 5 -> level != null && level.isClientSide ? clientProcessingTicks : processingTicks();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progressTime = value;
                case 1 -> fuelTime = value;
                case 2 -> fuelDuration = value;
                case 3 -> clientWashAmount = value;
                case 4 -> clientRequiredAmount = value;
                case 5 -> clientProcessingTicks = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private int clientProcessingTicks = PROCESSING_TICKS;

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
        still.acceptContainer();
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
        if (++still.progressTime >= still.processingTicks()) {
            ItemStack result = still.outputForBatch();
            if (still.activeRecipe != null) still.wash.shrink(still.requiredWashAmount());
            else still.items.get(WORT_SLOT).shrink(1);
            still.items.get(BOTTLE_SLOT).shrink(1);
            ItemStack output = still.items.get(OUTPUT_SLOT);
            if (output.isEmpty()) {
                still.items.set(OUTPUT_SLOT, result);
            } else {
                output.grow(1);
            }
            still.progressTime = 0;
            still.activeRecipe = null;
        }
        still.setChanged();
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) { return washHandler; }

    private boolean acceptsWash(FluidStack fluid) {
        if (level == null || fluid.isEmpty()) return false;
        return level.getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .map(holder -> holder.value()).anyMatch(recipe -> recipe.inputFluid().equals(BuiltInRegistries.FLUID.getKey(fluid.getFluid()))
                        && recipe.requiredInputAmount(fluid) <= WASH_CAPACITY);
    }

    private void acceptContainer() {
        ItemStack held = items.get(WORT_SLOT);
        ProductionContainers.read(held).ifPresent(fluid -> {
            if (washHandler.fill(fluid, FluidAction.SIMULATE) == fluid.getAmount()) {
                washHandler.fill(fluid, FluidAction.EXECUTE);
                ItemStack empty = ProductionContainers.emptyContainer(held);
                held.shrink(1);
                if (held.isEmpty()) items.set(WORT_SLOT, empty);
                else if (!empty.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + 1, worldPosition.getZ() + .5, empty);
            }
        });
    }

    private Optional<DistillationRecipe> findRecipe() {
        if (level == null || wash.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get()).stream()
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .map(holder -> holder.value()).filter(recipe -> recipe.matches(wash)).findFirst();
    }

    public int requiredWashAmount() { return activeRecipe == null ? 0 : activeRecipe.requiredInputAmount(wash); }
    public int processingTicks() { return activeRecipe == null ? PROCESSING_TICKS : activeRecipe.durationTicks(); }

    private ItemStack outputForBatch() { return activeRecipe == null ? resultTemplate.copy() : activeRecipe.output(wash); }

    public boolean canStill() {
        if (!items.get(BOTTLE_SLOT).is(ModItems.FIFTH_BOTTLE_EMPTY.get())) return false;
        if (activeRecipe == null) activeRecipe = findRecipe().orElse(null);
        if (activeRecipe != null && !activeRecipe.matches(wash)) return false;
        if (activeRecipe == null && !items.get(WORT_SLOT).is(ModItems.FERMENTED_AGAVE_WORT.get())) return false;
        ItemStack result = outputForBatch();
        if (result.isEmpty()) return false;
        ItemStack output = items.get(OUTPUT_SLOT);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() < getMaxStackSize(output);
    }

    /** Recover even a partial bucket before the assembled machine is dismantled. */
    public void dropWash() {
        FluidStack saved = wash;
        wash = FluidStack.EMPTY;
        if (level == null || level.isClientSide) return;
        while (!saved.isEmpty()) {
            int amount = Math.min(ProductionContainers.BUCKET_VOLUME, saved.getAmount());
            Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5,
                    ProductionContainers.create(saved.copyWithAmount(amount)));
            saved.shrink(amount);
        }
        setChanged();
    }

    public static boolean isValidInput(int slot, ItemStack stack) {
        return switch (slot) {
            case WORT_SLOT -> stack.is(ModItems.FERMENTED_AGAVE_WORT.get()) || stack.is(ModItems.BATCH_BUCKET.get());
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
        return slot == OUTPUT_SLOT || slot == FUEL_SLOT && !isValidInput(FUEL_SLOT, stack)
                || slot == WORT_SLOT && stack.is(net.minecraft.world.item.Items.BUCKET);
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
        wash = FluidStack.EMPTY;
        activeRecipe = null;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        fuelTime = Math.max(0, tag.getInt("FuelTime"));
        wash = FluidStack.parseOptional(registries, tag.getCompound("Wash"));
        if (wash.getAmount() > WASH_CAPACITY) wash.setAmount(WASH_CAPACITY);
        activeRecipe = DistillationRecipe.CODEC.codec().parse(NbtOps.INSTANCE, tag.getCompound("DistillationPlan")).result().orElse(null);
        progressTime = Math.clamp(tag.getInt("ProgressTime"), 0, processingTicks() - 1);
        fuelDuration = Math.max(fuelTime, tag.getInt("FuelDuration"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        if (!wash.isEmpty()) tag.put("Wash", wash.save(registries));
        if (activeRecipe != null) DistillationRecipe.CODEC.codec().encodeStart(NbtOps.INSTANCE, activeRecipe).result().ifPresent(plan -> tag.put("DistillationPlan", plan));
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
