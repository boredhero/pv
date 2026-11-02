package io.vinum.tileentity;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.BarrelContents;
import io.vinum.block.WineBarrelBlock;
import io.vinum.config.BarrelConfig;
import io.vinum.multiblock.BarrelMultiblock;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Each physical barrel owns only its local fluid escrow. A formed cube exposes an
 * aggregate view; no controller owns its neighbors' saved contents. Unloaded or
 * inconsistent groups reject transfers without loading chunks or moving fluid.
 */
public class BarrelBlockEntity extends BlockEntity {
    private FluidStack localFluid = FluidStack.EMPTY;
    private long ageReferenceTick = -1;
    private BlockPos structureOrigin;
    private int structureSize = 1;
    private final IFluidHandler fluidHandler = new AggregateHandler();
    private Snapshot cachedSnapshot;
    private long cachedSnapshotTick = Long.MIN_VALUE;

    public BarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModTileEntities.BARREL.get(), pos, state);
        structureOrigin = pos.immutable();
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) { return fluidHandler; }
    public BlockPos getStructureOrigin() { return structureOrigin; }
    public int getStructureSize() { return structureSize; }
    public boolean isController() { return worldPosition.equals(structureOrigin); }
    public int getCapacity() { return BarrelConfig.capacityPerBlock() * structureSize * structureSize * structureSize; }
    public boolean isStructureLoaded() { return members() != null; }

    public FluidStack getLocalFluid() {
        return BarrelFluidData.withAge(localFluid, getLocalAge());
    }

    public long getLocalAge() {
        if (localFluid.isEmpty()) return 0;
        return BarrelFluidData.elapsedAge(BarrelFluidData.getAge(localFluid), ageReferenceTick,
                level == null ? ageReferenceTick : level.getGameTime());
    }

    public FluidStack getFluid() { return fluidHandler.getFluidInTank(0); }

    public BarrelContents getContents() {
        Snapshot snapshot = snapshot();
        return snapshot == null ? new BarrelContents(FluidStack.EMPTY, getCapacity(), 0, false)
                : new BarrelContents(snapshot.fluid, getCapacity(), BarrelFluidData.getAge(snapshot.fluid), true);
    }

    public Component getStatusMessage() {
        Snapshot snapshot = snapshot();
        if (snapshot == null) return Component.translatableWithFallback("barrel.vinum.unavailable",
                "Oak barrel · Structure unavailable (neighboring chunks are not loaded)");
        String dimensions = structureSize + "×" + structureSize + "×" + structureSize;
        if (snapshot.fluid.isEmpty()) return Component.translatableWithFallback("barrel.vinum.empty",
                "Oak barrel · Empty · 0 / %s L · %s", liters(getCapacity()), dimensions);
        String days = String.format(Locale.ROOT, "%.1f",
                BarrelFluidData.getAge(snapshot.fluid) / (double) BarrelConfig.ticksPerDay());
        return Component.translatableWithFallback("barrel.vinum.contents",
                "Oak barrel · %s · %s / %s L · Age %s days · %s", snapshot.fluid.getHoverName(),
                liters(snapshot.amount), liters(getCapacity()), days, dimensions);
    }

    private static String liters(int amount) {
        return BigDecimal.valueOf(amount, 3).stripTrailingZeros().toPlainString();
    }

    /**
     * Mix a newly formed reservoir once, without moving any member's fluid escrow.
     * A subsequent split or harvested part therefore retains the blended batch age,
     * rather than allowing the older fraction to be recovered from a mixed batch.
     */
    public static boolean normalizeMergedAge(List<BarrelBlockEntity> members) {
        if (members.isEmpty()) return false;
        Level level = members.getFirst().getLevel();
        if (level == null || level.isClientSide) return false;
        FluidStack representative = FluidStack.EMPTY;
        long amount = 0;
        BigInteger totalAge = BigInteger.ZERO;
        // Validate and calculate the entire batch before changing any member.
        for (BarrelBlockEntity member : members) {
            if (member.getLevel() != level || member.isRemoved() || !level.hasChunkAt(member.worldPosition)
                    || level.getBlockEntity(member.worldPosition) != member) return false;
            FluidStack fluid = member.getLocalFluid();
            if (fluid.isEmpty()) continue;
            if (!representative.isEmpty() && !BarrelFluidData.sameFluid(representative, fluid)) return false;
            representative = fluid;
            amount += fluid.getAmount();
            totalAge = totalAge.add(BigInteger.valueOf(BarrelFluidData.getAge(fluid))
                    .multiply(BigInteger.valueOf(fluid.getAmount())));
        }
        if (amount == 0) return true;
        long blendedAge = totalAge.divide(BigInteger.valueOf(amount)).longValueExact();
        for (BarrelBlockEntity member : members) {
            if (!member.localFluid.isEmpty() && member.getLocalAge() != blendedAge) {
                member.setLocal(member.localFluid, blendedAge);
            }
        }
        return true;
    }

    public void setStructure(BlockPos origin, int size) {
        size = Math.clamp(size, 1, BarrelMultiblock.MAX_SIZE);
        if (!BarrelMultiblock.contains(origin, size, worldPosition)) {
            origin = worldPosition;
            size = 1;
        }
        if (structureSize == size && structureOrigin.equals(origin)) return;
        invalidateSnapshot();
        structureOrigin = origin.immutable();
        structureSize = size;
        invalidateSnapshot();
        invalidateCapabilities();
        changed(true);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (ageReferenceTick < 0) ageReferenceTick = level.getGameTime();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            if (ageReferenceTick < 0) ageReferenceTick = level.getGameTime();
            BarrelMultiblock.enqueue(level, worldPosition);
        }
    }

    private void setLocal(FluidStack fluid, long age) {
        boolean changedIdentity = localFluid.isEmpty() != fluid.isEmpty()
                || !localFluid.isEmpty() && !BarrelFluidData.sameFluid(localFluid, fluid);
        localFluid = BarrelFluidData.withAge(fluid, age);
        ageReferenceTick = level == null ? -1 : level.getGameTime();
        invalidateSnapshot();
        // The renderer depends only on structure metadata. Server-side inspection and
        // automation read current contents directly, avoiding hundreds of packets for
        // every small transfer through a large reservoir.
        changed(false);
        if (changedIdentity && level != null && structureSize < BarrelMultiblock.MAX_SIZE) {
            // One aggregate transfer may touch hundreds of parts; only one expansion
            // search is necessary, and a maximum-sized cube cannot expand further.
            BarrelMultiblock.enqueue(level, structureOrigin);
        }
    }

    private void changed(boolean sync) {
        setChanged();
        if (sync && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void invalidateSnapshot() {
        cachedSnapshot = null;
        if (level != null && level.hasChunkAt(structureOrigin)
                && level.getBlockEntity(structureOrigin) instanceof BarrelBlockEntity controller) {
            controller.cachedSnapshot = null;
        }
    }

    @Nullable
    private List<BarrelBlockEntity> members() {
        if (level == null || isRemoved()) return null;
        List<BarrelBlockEntity> result = new ArrayList<>(structureSize * structureSize * structureSize);
        Direction facing = getBlockState().getValue(WineBarrelBlock.FACING);
        for (BlockPos cursor : BlockPos.betweenClosed(structureOrigin,
                structureOrigin.offset(structureSize - 1, structureSize - 1, structureSize - 1))) {
            if (!level.hasChunkAt(cursor)) return null;
            if (!(level.getBlockEntity(cursor) instanceof BarrelBlockEntity member) || member.isRemoved()
                    || member.structureSize != structureSize || !member.structureOrigin.equals(structureOrigin)
                    || member.getBlockState().getValue(WineBarrelBlock.FACING) != facing) return null;
            result.add(member);
        }
        return result;
    }

    @Nullable
    private Snapshot snapshot() {
        List<BarrelBlockEntity> members = members();
        if (members == null) return null;
        BarrelBlockEntity controller = members.getFirst();
        long now = level.getGameTime();
        // Membership is still checked on every access, so cached reads never mask an
        // unloaded/replaced member. Repeated SIMULATE calls reuse the expensive age sum.
        if (controller.cachedSnapshot != null && controller.cachedSnapshotTick == now
                && controller.cachedSnapshot.members.equals(members)) return controller.cachedSnapshot;
        FluidStack representative = FluidStack.EMPTY;
        long amount = 0;
        BigInteger totalAge = BigInteger.ZERO;
        for (BarrelBlockEntity member : members) {
            FluidStack fluid = member.getLocalFluid();
            if (fluid.isEmpty()) continue;
            if (!representative.isEmpty() && !BarrelFluidData.sameFluid(representative, fluid)) return null;
            representative = fluid;
            amount += fluid.getAmount();
            totalAge = totalAge.add(BigInteger.valueOf(BarrelFluidData.getAge(fluid)).multiply(BigInteger.valueOf(fluid.getAmount())));
        }
        if (amount > Integer.MAX_VALUE) return null;
        FluidStack fluid = representative.isEmpty() ? FluidStack.EMPTY
                : BarrelFluidData.withAge(representative, totalAge.divide(BigInteger.valueOf(amount)).longValueExact());
        if (!fluid.isEmpty()) fluid.setAmount((int) amount);
        Snapshot snapshot = new Snapshot(members, fluid, (int) amount);
        controller.cachedSnapshot = snapshot;
        controller.cachedSnapshotTick = now;
        return snapshot;
    }

    private record Snapshot(List<BarrelBlockEntity> members, FluidStack fluid, int amount) {}

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", localFluid.saveOptional(registries));
        tag.putLong("AgeReferenceTick", ageReferenceTick);
        tag.putLong("StructureOrigin", structureOrigin.asLong());
        tag.putInt("StructureSize", structureSize);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        invalidateSnapshot();
        super.loadAdditional(tag, registries);
        localFluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
        ageReferenceTick = tag.contains("AgeReferenceTick") ? tag.getLong("AgeReferenceTick") : -1;
        structureOrigin = tag.contains("StructureOrigin") ? BlockPos.of(tag.getLong("StructureOrigin")) : worldPosition;
        structureSize = Math.clamp(tag.getInt("StructureSize"), 1, BarrelMultiblock.MAX_SIZE);
        if (!BarrelMultiblock.contains(structureOrigin, structureSize, worldPosition)) {
            structureOrigin = worldPosition;
            structureSize = 1;
        }
        if (ageReferenceTick < 0 && level != null) ageReferenceTick = level.getGameTime();
    }

    @Override
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        if (localFluid.isEmpty()) {
            // Empty barrels need no reservoir payload and should stack normally.
            // Leave names, lore and other components added by loot modifiers intact.
            stack.remove(DataComponents.BLOCK_ENTITY_DATA);
            return;
        }
        CompoundTag tag = saveCustomOnly(registries);
        tag.put("Fluid", getLocalFluid().saveOptional(registries));
        tag.putLong("AgeReferenceTick", -1);
        tag.remove("StructureOrigin");
        tag.remove("StructureSize");
        BlockItem.setBlockEntityData(stack, getType(), tag);
        stack.applyComponents(collectComponents());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private final class AggregateHandler implements IFluidHandler {
        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) {
            Snapshot snapshot = tank == 0 ? snapshot() : null;
            return snapshot == null ? FluidStack.EMPTY : snapshot.fluid.copy();
        }

        @Override
        public int getTankCapacity(int tank) { return tank == 0 ? getCapacity() : 0; }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) return false;
            Snapshot snapshot = snapshot();
            return snapshot != null && (snapshot.fluid.isEmpty() || BarrelFluidData.sameFluid(snapshot.fluid, stack));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || level == null || level.isClientSide) return 0;
            Snapshot snapshot = snapshot();
            if (snapshot == null || !snapshot.fluid.isEmpty() && !BarrelFluidData.sameFluid(snapshot.fluid, resource)) return 0;
            int accepted = Math.min(resource.getAmount(), Math.max(0, getCapacity() - snapshot.amount));
            if (accepted <= 0 || action.simulate()) return accepted;
            long age = BarrelFluidData.weightedAge(BarrelFluidData.getAge(snapshot.fluid), snapshot.amount,
                    BarrelFluidData.getAge(resource), accepted);
            long free = 0;
            for (BarrelBlockEntity member : snapshot.members) free += Math.max(0, BarrelConfig.capacityPerBlock() - member.localFluid.getAmount());
            int remaining = accepted;
            long remainingSpace = free;
            for (BarrelBlockEntity member : snapshot.members) {
                int space = Math.max(0, BarrelConfig.capacityPerBlock() - member.localFluid.getAmount());
                int share = remainingSpace == 0 ? 0 : (int) ((long) remaining * space / remainingSpace);
                remainingSpace -= space;
                remaining -= share;
                int newAmount = member.localFluid.getAmount() + share;
                if (newAmount > 0) {
                    FluidStack local = resource.copyWithAmount(newAmount);
                    member.setLocal(local, age);
                }
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            Snapshot snapshot = snapshot();
            if (snapshot == null || !BarrelFluidData.sameFluid(snapshot.fluid, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || level == null || level.isClientSide) return FluidStack.EMPTY;
            Snapshot snapshot = snapshot();
            if (snapshot == null || snapshot.fluid.isEmpty()) return FluidStack.EMPTY;
            int drained = Math.min(maxDrain, snapshot.amount);
            FluidStack result = snapshot.fluid.copyWithAmount(drained);
            if (action.simulate()) return result;
            int remaining = drained;
            int remainingAmount = snapshot.amount;
            long age = BarrelFluidData.getAge(snapshot.fluid);
            for (BarrelBlockEntity member : snapshot.members) {
                int localAmount = member.localFluid.getAmount();
                int share = remainingAmount == 0 ? 0 : (int) ((long) remaining * localAmount / remainingAmount);
                remainingAmount -= localAmount;
                remaining -= share;
                if (localAmount > 0) member.setLocal(localAmount == share ? FluidStack.EMPTY
                        : snapshot.fluid.copyWithAmount(localAmount - share), age);
            }
            return result;
        }
    }
}
