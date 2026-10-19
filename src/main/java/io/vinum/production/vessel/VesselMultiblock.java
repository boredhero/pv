package io.vinum.production.vessel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Bounded square-footprint assemblies. Fluid never leaves the physical part that owns it. */
public final class VesselMultiblock {
    private VesselMultiblock() {}
    public static boolean contains(BlockPos origin, int size, BlockPos pos) {
        return contains(origin, size, size, pos);
    }
    public static boolean contains(BlockPos origin, int width, int height, BlockPos pos) {
        return pos.getX() >= origin.getX() && pos.getX() < origin.getX() + width
                && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + height
                && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + width;
    }
    private static BlockPos end(FermentationVesselBlockEntity part) {
        return part.getStructureOrigin().offset(part.getStructureSize() - 1, part.getStructureHeight() - 1, part.getStructureSize() - 1);
    }
    private static boolean sameStructure(FermentationVesselBlockEntity part, BlockPos origin, int width, int height) {
        return part.getStructureOrigin().equals(origin) && part.getStructureSize() == width && part.getStructureHeight() == height;
    }
    private static FermentationVesselBlockEntity vat(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity be && !be.isRemoved() && be.isVat() ? be : null;
    }
    public static void removed(Level level, FermentationVesselBlockEntity removed) {
        if (level.isClientSide) return;
        for (BlockPos pos : BlockPos.betweenClosed(removed.getStructureOrigin(), end(removed))) {
            var part = vat(level, pos);
            if (part != null && part != removed && sameStructure(part, removed.getStructureOrigin(), removed.getStructureSize(), removed.getStructureHeight())) {
                part.setStructure(pos, 1);
                level.scheduleTick(pos, part.getBlockState().getBlock(), 1);
            }
        }
        for (Direction direction : Direction.values()) {
            var part = vat(level, removed.getBlockPos().relative(direction));
            if (part != null) level.scheduleTick(part.getBlockPos(), part.getBlockState().getBlock(), 1);
        }
    }
    private record Candidate(BlockPos origin, int width, int height) {
        int volume() { return width * width * height; }
    }
    public static void reconcile(ServerLevel level, BlockPos changedPos) {
        var changed = vat(level, changedPos);
        if (changed == null) return;
        // An unloaded neighbor is unavailable, not broken. onLoad schedules validation again.
        boolean unavailable = false;
        boolean broken = false;
        BlockPos oldOrigin = changed.getStructureOrigin();
        int oldWidth = changed.getStructureSize(), oldHeight = changed.getStructureHeight();
        for (BlockPos pos : BlockPos.betweenClosed(oldOrigin, end(changed))) {
            if (!level.hasChunkAt(pos)) { unavailable = true; continue; }
            var part = vat(level, pos);
            if (part == null || !sameStructure(part, oldOrigin, oldWidth, oldHeight)) broken = true;
        }
        if (broken) {
            for (BlockPos pos : BlockPos.betweenClosed(oldOrigin, end(changed))) {
                var part = vat(level, pos);
                if (part != null && sameStructure(part, oldOrigin, oldWidth, oldHeight)) part.setStructure(pos, 1);
            }
        } else if (unavailable || oldWidth * oldWidth * oldHeight == 27) return;
        List<Candidate> candidates = new ArrayList<>();
        int currentVolume = changed.getStructureSize() * changed.getStructureSize() * changed.getStructureHeight();
        for (int width = 1; width <= 3; width++) for (int height = 1; height <= 3; height++) {
            if (width * width * height <= currentVolume) continue;
            for (int x = 1 - width; x <= 0; x++) for (int y = 1 - height; y <= 0; y++) for (int z = 1 - width; z <= 0; z++) {
                candidates.add(new Candidate(changedPos.offset(x, y, z), width, height));
            }
        }
        // Largest volume wins; equal volumes prefer wider footprints, then the lowest x/y/z origin.
        candidates.sort(Comparator.comparingInt(Candidate::volume).reversed()
                .thenComparing(Comparator.comparingInt(Candidate::width).reversed())
                .thenComparingInt(c -> c.origin().getX()).thenComparingInt(c -> c.origin().getY()).thenComparingInt(c -> c.origin().getZ()));
        for (Candidate candidate : candidates) {
            BlockPos origin = candidate.origin();
            int width = candidate.width(), height = candidate.height();
            List<FermentationVesselBlockEntity> parts = new ArrayList<>(candidate.volume());
            FluidStack representative = FluidStack.EMPTY;
            boolean valid = true;
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, height - 1, width - 1))) {
                var part = vat(level, pos);
                if (part == null || part.isFermenting()
                        || !contains(origin, width, height, part.getStructureOrigin())
                        || !contains(origin, width, height, end(part)) || part.members() == null) { valid = false; break; }
                FluidStack local = part.getLocalFluid();
                if (!local.isEmpty()) {
                    if (!representative.isEmpty() && !FluidStack.isSameFluidSameComponents(representative, local)) { valid = false; break; }
                    representative = local;
                }
                parts.add(part);
            }
            if (!valid) continue;
            for (var part : parts) part.setStructure(origin, width, height);
            return;
        }
    }
}
