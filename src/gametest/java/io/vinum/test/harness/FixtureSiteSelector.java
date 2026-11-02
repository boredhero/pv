package io.vinum.test.harness;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded search for disposable test space; never loads chunks or replaces existing blocks. */
public final class FixtureSiteSelector {
    private FixtureSiteSelector() {}

    public static BlockPos find(ServerPlayer player) {
        var level = player.serverLevel();
        return find(player.blockPosition(), level.getMinBuildHeight(), level.getMaxBuildHeight(), target -> {
            for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 3, 3))) {
                if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                        || !level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).isAir()) return false;
            }
            return level.getEntities(player, new AABB(Vec3.atLowerCornerOf(target.offset(-3, -1, -3)),
                    Vec3.atLowerCornerOf(target.offset(4, 4, 4)))).isEmpty();
        });
    }

    /** Predicate receives candidate centers only within build-height limits. */
    public static BlockPos find(BlockPos origin, int minHeight, int maxHeight, Predicate<BlockPos> available) {
        int[] offsets = {0, 8, -8, 16, -16};
        for (int rise : new int[] {8, 16, 24, 32, 48, 64}) {
            int y = Math.min(origin.getY() + rise, maxHeight - 4);
            if (y < minHeight + 1) continue;
            for (int x : offsets) for (int z : offsets) {
                BlockPos target = new BlockPos(origin.getX() + x, y, origin.getZ() + z);
                if (available.test(target)) return target;
            }
        }
        throw new IllegalStateException("No loaded, empty, entity-free 7×5×7 fixture space found within 16 blocks horizontally and 64 above you; move to an open area and retry");
    }
}
