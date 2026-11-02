package io.vinum.block;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

/** Bounded, geometry-derived connectivity for the at-most 3x3x3 plant. */
public final class AgaveHarvest {
  private static final ThreadLocal<Boolean> CLEANING = ThreadLocal.withInitial(() -> false);

  private AgaveHarvest() {}

  public static void removeDetachedLeaves(Level level, BlockPos root) {
    if (level.isClientSide || CLEANING.get()) return;
    var state = level.getBlockState(root);
    if (!(state.getBlock() instanceof AgaveCropBlock) || state.getValue(AgaveCropBlock.AGE) != 7)
      return;
    var cells = AgaveGeometry.cells(7);
    var present = new HashSet<BlockPos>();
    present.add(BlockPos.ZERO);
    for (var cell : cells) {
      var pos = root.offset(cell);
      // Unloaded sections are unknown, not severed. A later loaded tick retries cleanup.
      if (!level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()),
          SectionPos.blockToSectionCoord(pos.getZ()))) return;
      if (AgaveLeafBlock.belongsTo(level.getBlockState(pos), pos, root)) present.add(cell);
    }
    var attached = new HashSet<BlockPos>();
    var queue = new ArrayDeque<BlockPos>();
    attached.add(BlockPos.ZERO);
    queue.add(BlockPos.ZERO);
    while (!queue.isEmpty()) {
      for (var next : AgaveGeometry.connections(7, queue.removeFirst())) {
        if (present.contains(next) && attached.add(next)) queue.addLast(next);
      }
    }
    CLEANING.set(true);
    try {
      for (var cell : present)
        if (!attached.contains(cell)) level.removeBlock(root.offset(cell), false);
      AgaveCropBlock.refreshStripped(level, root);
    } finally {
      CLEANING.remove();
    }
  }
}
