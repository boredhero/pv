package io.vinum.tree;

import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** A server-game-time deadline, persisted even when the log is carried or its chunk unloaded. */
public final class CorkLogBlockEntity extends BlockEntity {
  private long readyAt = -1;

  public CorkLogBlockEntity(BlockPos pos, BlockState state) {
    super(CorkOakContent.LOG_ENTITY.get(), pos, state);
  }

  public boolean harvested() {
    return readyAt >= 0;
  }

  public long readyAt() {
    return readyAt;
  }

  public boolean harvest() {
    if (level == null || level.isClientSide || harvested()) return false;
    readyAt = Math.addExact(level.getGameTime(), CorkOakConfig.regrowthTicks());
    level.setBlock(worldPosition, getBlockState().setValue(CorkLogBlock.HARVESTED, true), 3);
    setChanged();
    schedule();
    return true;
  }

  /**
   * Detached lumber cannot regenerate bark: a bounded connected trunk must still reach soil and
   * leaves.
   */
  public boolean isLivingTree() {
    if (level == null) return false;
    var pending = new ArrayDeque<BlockPos>();
    var visited = new HashSet<BlockPos>();
    pending.add(worldPosition);
    boolean rooted = false, leafy = false;
    while (!pending.isEmpty() && visited.size() < 256) {
      BlockPos pos = pending.removeFirst();
      if (!visited.add(pos)) continue;
      for (Direction side : Direction.values()) {
        BlockPos next = pos.relative(side);
        if (level.getChunk(
                SectionPos.blockToSectionCoord(next.getX()),
                SectionPos.blockToSectionCoord(next.getZ()),
                ChunkStatus.FULL,
                false)
            == null) continue;
        BlockState state = level.getBlockState(next);
        if (side == Direction.DOWN
            && (state.is(BlockTags.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.FARMLAND))) rooted = true;
        if (state.is(CorkOakContent.LEAVES.get())) leafy = true;
        if (state.is(CorkOakContent.LOG.get()) && !visited.contains(next)) pending.add(next);
      }
      if (rooted && leafy) return true;
    }
    return false;
  }

  public boolean refresh() {
    if (level == null
        || level.isClientSide
        || !harvested()
        || level.getGameTime() < readyAt
        || !isLivingTree()) return false;
    readyAt = -1;
    setChanged();
    level.setBlock(worldPosition, getBlockState().setValue(CorkLogBlock.HARVESTED, false), 3);
    return true;
  }

  public void schedule() {
    if (level == null || level.isClientSide || !harvested()) return;
    int delay = nextCheckDelay(level.getGameTime());
    level.scheduleTick(worldPosition, CorkOakContent.LOG.get(), delay);
  }

  public int nextCheckDelay(long now) {
    return readyAt <= now ? 1200 : (int) Math.clamp(readyAt - now, 1, 1200);
  }

  @Override
  public void onLoad() {
    super.onLoad();
    schedule();
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putLong("CorkReadyAt", readyAt);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    readyAt = tag.contains("CorkReadyAt") ? Math.max(-1, tag.getLong("CorkReadyAt")) : -1;
  }

  @Override
  public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
    if (harvested()) BlockItem.setBlockEntityData(stack, getType(), saveCustomOnly(registries));
  }
}
