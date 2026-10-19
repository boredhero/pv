package io.vinum.production.vessel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Event-driven, bounded three-cube assembly. Fluid never leaves the physical part that owns it. */
public final class VesselMultiblock {
    private VesselMultiblock() {}
    public static boolean contains(BlockPos origin, int size, BlockPos pos) {
        return pos.getX() >= origin.getX() && pos.getX() < origin.getX() + size
                && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + size
                && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + size;
    }
    private static FermentationVesselBlockEntity vat(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity be && be.isVat() ? be : null;
    }
    public static void removed(Level level, FermentationVesselBlockEntity removed) {
        if (level.isClientSide) return;
        for (BlockPos pos : BlockPos.betweenClosed(removed.getStructureOrigin(), removed.getStructureOrigin()
                .offset(removed.getStructureSize() - 1, removed.getStructureSize() - 1, removed.getStructureSize() - 1))) {
            var part = vat(level, pos);
            if (part != null && part != removed) {
                part.setStructure(pos, 1);
                level.scheduleTick(pos, part.getBlockState().getBlock(), 1);
            }
        }
        for (Direction direction : Direction.values()) {
            var part = vat(level, removed.getBlockPos().relative(direction));
            if (part != null) level.scheduleTick(part.getBlockPos(), part.getBlockState().getBlock(), 1);
        }
    }
    public static void reconcile(ServerLevel level, BlockPos changedPos) {
        var changed = vat(level, changedPos);
        if (changed == null) return;
        // An unloaded neighbor is unavailable, not broken. onLoad schedules validation again.
        boolean unavailable = false;
        boolean broken = false;
        BlockPos oldOrigin = changed.getStructureOrigin();
        int oldSize = changed.getStructureSize();
        for (BlockPos pos : BlockPos.betweenClosed(oldOrigin, oldOrigin.offset(oldSize - 1, oldSize - 1, oldSize - 1))) {
            if (!level.hasChunkAt(pos)) { unavailable = true; continue; }
            var part = vat(level, pos);
            if (part == null || part.getStructureSize() != oldSize || !part.getStructureOrigin().equals(oldOrigin)) broken = true;
        }
        if (broken) {
            for (BlockPos pos : BlockPos.betweenClosed(oldOrigin, oldOrigin.offset(oldSize - 1, oldSize - 1, oldSize - 1))) {
                var part = vat(level, pos);
                if (part != null && part.getStructureOrigin().equals(oldOrigin)) part.setStructure(pos, 1);
            }
        } else if (unavailable || oldSize == 3) return;
        for (int size = 3; size > changed.getStructureSize(); size--) {
            for (int x = 1 - size; x <= 0; x++) for (int y = 1 - size; y <= 0; y++) for (int z = 1 - size; z <= 0; z++) {
                BlockPos origin = changedPos.offset(x, y, z);
                List<FermentationVesselBlockEntity> parts = new ArrayList<>(size * size * size);
                FluidStack representative = FluidStack.EMPTY;
                boolean valid = true;
                for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
                    var part = vat(level, pos);
                    if (part == null || part.isFermenting()
                            || !contains(origin, size, part.getStructureOrigin())
                            || !contains(origin, size, part.getStructureOrigin().offset(part.getStructureSize() - 1, part.getStructureSize() - 1, part.getStructureSize() - 1))) { valid = false; break; }
                    FluidStack local = part.getLocalFluid();
                    if (!local.isEmpty()) {
                        if (!representative.isEmpty() && !FluidStack.isSameFluidSameComponents(representative, local)) { valid = false; break; }
                        representative = local;
                    }
                    parts.add(part);
                }
                if (!valid) continue;
                for (var part : parts) part.setStructure(origin, size);
                return;
            }
        }
    }
}
