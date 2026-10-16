package io.vinum.multiblock;

import io.vinum.block.WineBarrelBlock;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.common.Defines;
import io.vinum.tileentity.BarrelBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Forms complete, consistently facing barrel cubes without moving their fluid escrow.
 * Only changed neighborhoods are searched. A 17-cubed snapshot and summed-volume table
 * make each candidate completeness check constant time, including even-sized cubes.
 */
@EventBusSubscriber(modid = Defines.MODID)
public final class BarrelMultiblock {
    public static final int MAX_SIZE = 9;
    private static final int RADIUS = MAX_SIZE - 1;
    private static final int WIDTH = RADIUS * 2 + 1;
    private static final int PREFIX_WIDTH = WIDTH + 1;
    private static final int SEARCHES_PER_TICK = 8;
    private static final Map<ServerLevel, LinkedHashSet<BlockPos>> PENDING = new WeakHashMap<>();

    private BarrelMultiblock() {}

    /** Coalesced across repeated placement, neighbor and chunk-load notifications. */
    public static void enqueue(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) {
            PENDING.computeIfAbsent(server, ignored -> new LinkedHashSet<>()).add(pos.immutable());
        }
    }

    @SubscribeEvent
    public static void afterLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        LinkedHashSet<BlockPos> pending = PENDING.get(level);
        if (pending == null || pending.isEmpty()) return;
        for (int i = 0; i < SEARCHES_PER_TICK && !pending.isEmpty(); i++) {
            BlockPos pos = pending.iterator().next();
            pending.remove(pos);
            reconcile(level, pos);
        }
        if (pending.isEmpty()) PENDING.remove(level);
    }

    /** Public synchronous entry point for structure tools and integration tests. */
    public static void reconcile(ServerLevel level, BlockPos changedPos) {
        BarrelBlockEntity changed = barrelAt(level, changedPos);
        if (changed == null) return;
        validateExisting(level, changed);
        Direction facing = changed.getBlockState().getValue(WineBarrelBlock.FACING);
        int oldSize = changed.getStructureSize();
        if (oldSize == MAX_SIZE) {
            discardPending(level, changed.getStructureOrigin(), oldSize);
            return;
        }
        // Every larger cube contains a 2x2x2 cube around this member. Isolated
        // additions and thin unfinished layers therefore need only 27 block reads.
        if (oldSize == 1 && !hasCompleteTwoCube(level, changedPos, facing)) return;
        Snapshot snapshot = new Snapshot(level, changedPos, facing);
        for (int size = MAX_SIZE; size > Math.max(1, oldSize); size--) {
            // An expanding cube must contain the changed block. The snapshot has enough
            // margin for every possible origin, rather than assuming odd sizes or centers.
            for (int y = RADIUS - size + 1; y <= RADIUS; y++) {
                for (int z = RADIUS - size + 1; z <= RADIUS; z++) {
                    for (int x = RADIUS - size + 1; x <= RADIUS; x++) {
                        if (snapshot.count(x, y, z, size) != size * size * size) continue;
                        BlockPos origin = snapshot.origin.offset(x, y, z);
                        List<BarrelBlockEntity> members = snapshot.compatibleMembers(x, y, z, size, origin);
                        if (members == null || !BarrelBlockEntity.normalizeMergedAge(members)) continue;
                        // Joining reservoirs mixes the batch once. Only age is homogenized;
                        // each physical block retains the same volume for safe teardown.
                        for (BarrelBlockEntity member : members) member.setStructure(origin, size);
                        discardPending(level, origin, size);
                        return;
                    }
                }
            }
        }
        // A stable group's other queued members would repeat exactly this work. New
        // additions outside its bounds remain queued and can still trigger expansion.
        discardPending(level, changed.getStructureOrigin(), changed.getStructureSize());
    }

    /** Called while the departing entity still exists; ownership of fluid never changes. */
    public static void removed(Level level, BlockPos pos, BarrelBlockEntity removed) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos origin = removed.getStructureOrigin();
        int size = removed.getStructureSize();
        dissolve(server, origin, size, pos);
        for (Direction direction : Direction.values()) enqueue(level, pos.relative(direction));
    }

    private static void validateExisting(ServerLevel level, BarrelBlockEntity member) {
        int size = member.getStructureSize();
        BlockPos origin = member.getStructureOrigin();
        if (size < 1 || size > MAX_SIZE || !contains(origin, size, member.getBlockPos())) {
            member.setStructure(member.getBlockPos(), 1);
            return;
        }
        Direction facing = member.getBlockState().getValue(WineBarrelBlock.FACING);
        FluidStack fluid = FluidStack.EMPTY;
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            // Missing chunks are not evidence of a broken structure. Their entities enqueue
            // themselves on load; no validation path force-loads neighboring chunks.
            if (!level.hasChunkAt(pos)) continue;
            BarrelBlockEntity other = barrelAt(level, pos);
            if (other == null || other.getBlockState().getValue(WineBarrelBlock.FACING) != facing
                    || other.getStructureSize() != size || !other.getStructureOrigin().equals(origin)) {
                dissolve(level, origin, size, null);
                return;
            }
            FluidStack local = other.getLocalFluid();
            if (!local.isEmpty()) {
                if (!fluid.isEmpty() && !BarrelFluidData.sameFluid(fluid, local)) {
                    dissolve(level, origin, size, null);
                    return;
                }
                fluid = local;
            }
        }
    }

    private static void dissolve(ServerLevel level, BlockPos origin, int size, BlockPos except) {
        if (size < 1 || size > MAX_SIZE) return;
        for (BlockPos cursor : BlockPos.betweenClosed(origin, origin.offset(size - 1, size - 1, size - 1))) {
            if (cursor.equals(except)) continue;
            BarrelBlockEntity member = barrelAt(level, cursor);
            if (member != null && member.getStructureSize() == size && member.getStructureOrigin().equals(origin)) {
                member.setStructure(cursor.immutable(), 1);
                enqueue(level, cursor);
            }
        }
    }

    public static boolean contains(BlockPos origin, int size, BlockPos pos) {
        return pos.getX() >= origin.getX() && pos.getX() < origin.getX() + size
            && pos.getY() >= origin.getY() && pos.getY() < origin.getY() + size
            && pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + size;
    }

    private static boolean hasCompleteTwoCube(ServerLevel level, BlockPos center, Direction facing) {
        boolean[] present = new boolean[27];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    pos.setWithOffset(center, x - 1, y - 1, z - 1);
                    BarrelBlockEntity barrel = barrelAt(level, pos);
                    present[(y * 3 + z) * 3 + x] = barrel != null
                        && barrel.getBlockState().getValue(WineBarrelBlock.FACING) == facing;
                }
            }
        }
        for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
                for (int x = 0; x < 2; x++) {
                    boolean complete = true;
                    for (int dy = 0; dy < 2 && complete; dy++) {
                        for (int dz = 0; dz < 2 && complete; dz++) {
                            for (int dx = 0; dx < 2; dx++) {
                                if (!present[((y + dy) * 3 + z + dz) * 3 + x + dx]) {
                                    complete = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (complete) return true;
                }
            }
        }
        return false;
    }

    private static void discardPending(ServerLevel level, BlockPos origin, int size) {
        Set<BlockPos> pending = PENDING.get(level);
        if (pending != null) pending.removeIf(pos -> contains(origin, size, pos));
    }

    private static BarrelBlockEntity barrelAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof WineBarrelBlock && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel ? barrel : null;
    }

    private static final class Snapshot {
        private final BlockPos origin;
        private final BarrelBlockEntity[] barrels = new BarrelBlockEntity[WIDTH * WIDTH * WIDTH];
        private final int[] prefix = new int[PREFIX_WIDTH * PREFIX_WIDTH * PREFIX_WIDTH];

        private Snapshot(ServerLevel level, BlockPos center, Direction facing) {
            origin = center.offset(-RADIUS, -RADIUS, -RADIUS);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = 0; y < WIDTH; y++) {
                for (int z = 0; z < WIDTH; z++) {
                    for (int x = 0; x < WIDTH; x++) {
                        pos.setWithOffset(origin, x, y, z);
                        BarrelBlockEntity barrel = barrelAt(level, pos);
                        if (barrel != null && barrel.getBlockState().getValue(WineBarrelBlock.FACING) != facing) barrel = null;
                        barrels[index(x, y, z)] = barrel;
                        int px = x + 1, py = y + 1, pz = z + 1;
                        prefix[prefixIndex(px, py, pz)] = (barrel == null ? 0 : 1)
                            + value(px - 1, py, pz) + value(px, py - 1, pz) + value(px, py, pz - 1)
                            - value(px - 1, py - 1, pz) - value(px - 1, py, pz - 1) - value(px, py - 1, pz - 1)
                            + value(px - 1, py - 1, pz - 1);
                    }
                }
            }
        }

        private int count(int x, int y, int z, int size) {
            int xx = x + size, yy = y + size, zz = z + size;
            return value(xx, yy, zz) - value(x, yy, zz) - value(xx, y, zz) - value(xx, yy, z)
                + value(x, y, zz) + value(x, yy, z) + value(xx, y, z) - value(x, y, z);
        }

        private List<BarrelBlockEntity> compatibleMembers(int x, int y, int z, int size, BlockPos candidateOrigin) {
            List<BarrelBlockEntity> result = new ArrayList<>(size * size * size);
            FluidStack fluid = FluidStack.EMPTY;
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    for (int dx = 0; dx < size; dx++) {
                        BarrelBlockEntity member = barrels[index(x + dx, y + dy, z + dz)];
                        if (member == null) return null;
                        int oldSize = member.getStructureSize();
                        BlockPos oldOrigin = member.getStructureOrigin();
                        // Never absorb only a slice of an existing reservoir.
                        if (!contains(candidateOrigin, size, oldOrigin)
                                || !contains(candidateOrigin, size, oldOrigin.offset(oldSize - 1, oldSize - 1, oldSize - 1))) return null;
                        FluidStack local = member.getLocalFluid();
                        if (!local.isEmpty()) {
                            if (!fluid.isEmpty() && !BarrelFluidData.sameFluid(fluid, local)) return null;
                            fluid = local;
                        }
                        result.add(member);
                    }
                }
            }
            return result;
        }

        private int value(int x, int y, int z) { return prefix[prefixIndex(x, y, z)]; }
        private static int index(int x, int y, int z) { return (y * WIDTH + z) * WIDTH + x; }
        private static int prefixIndex(int x, int y, int z) { return (y * PREFIX_WIDTH + z) * PREFIX_WIDTH + x; }
    }
}
