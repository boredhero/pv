package io.vinum.crop;

import io.vinum.block.GrapeCropBlock;
import io.vinum.block.GrapeTrellisBlock;
import io.vinum.block.ModBlocks;
import io.vinum.tileentity.ModTileEntities;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Persistent ownership of one bounded row. Only the planted root may expand its list of sections.
 */
public final class GrapeVineBlockEntity extends BlockEntity {
  private BlockPos origin;
  private UUID vineId = UUID.randomUUID();
  private final Set<BlockPos> branches = new LinkedHashSet<>();
  private boolean retiring;

  public GrapeVineBlockEntity(BlockPos pos, BlockState state) {
    super(ModTileEntities.GRAPE_VINE.get(), pos, state);
    origin = pos.immutable();
  }

  public BlockPos origin() {
    return origin;
  }

  public int sectionCount() {
    return 1 + branches.size();
  }

  public boolean isRoot() {
    return origin.equals(worldPosition) && !getBlockState().getValue(GrapeCropBlock.BRANCH);
  }

  private static boolean loaded(Level level, BlockPos pos) {
    return level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) != null;
  }

  private GrapeVineBlockEntity owner() {
    if (level == null || !loaded(level, origin)) return null;
    return level.getBlockEntity(origin) instanceof GrapeVineBlockEntity root
            && root.isRoot()
            && root.vineId.equals(vineId)
            && root.getBlockState().is(getBlockState().getBlock())
        ? root
        : null;
  }

  private boolean owns(BlockPos pos) {
    return level != null
        && loaded(level, pos)
        && level.getBlockEntity(pos) instanceof GrapeVineBlockEntity section
        && section.origin.equals(worldPosition)
        && section.vineId.equals(vineId)
        && section.getBlockState().is(getBlockState().getBlock());
  }

  /** Null means a required chunk is unavailable; never treat that as a destroyed parent. */
  private Boolean connected(BlockPos target) {
    if (level == null) return null;
    int dx = target.getX() - worldPosition.getX(), dz = target.getZ() - worldPosition.getZ();
    if (target.getY() != worldPosition.getY()
        || dx != 0 && dz != 0
        || Math.abs(dx) + Math.abs(dz) >= VineyardConfig.MAX_SECTIONS) return false;
    Direction across = getBlockState().getValue(GrapeCropBlock.FACING).getClockWise();
    if (across.getAxis() == Direction.Axis.X ? dz != 0 : dx != 0) return false;
    int count = Math.abs(dx) + Math.abs(dz);
    for (int n = 1; n <= count; n++) {
      BlockPos pos = worldPosition.offset(Integer.signum(dx) * n, 0, Integer.signum(dz) * n);
      if (!loaded(level, pos)) return null;
      if (!branches.contains(pos) || !owns(pos)) return false;
      BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
      if (!lower.getValue(GrapeCropBlock.BRANCH)
          || lower.getValue(GrapeCropBlock.FACING)
              != getBlockState().getValue(GrapeCropBlock.FACING)
          || !upper.is(lower.getBlock())
          || upper.getValue(GrapeCropBlock.HALF) != DoubleBlockHalf.UPPER) return false;
    }
    return true;
  }

  public boolean ownershipReady() {
    if (isRoot()) return true;
    GrapeVineBlockEntity root = owner();
    return root != null && Boolean.TRUE.equals(root.connected(worldPosition));
  }

  /** Branches pause without their root chunk; orphaned sections restore their existing timber. */
  public boolean validateBranch() {
    if (isRoot()) return true;
    if (level == null || !loaded(level, origin)) return false;
    GrapeVineBlockEntity root = owner();
    Boolean linked = root == null ? false : root.connected(worldPosition);
    if (Boolean.FALSE.equals(linked)) restoreSupport();
    return Boolean.TRUE.equals(linked);
  }

  private void prune() {
    for (BlockPos pos : Set.copyOf(branches)) {
      if (Boolean.FALSE.equals(connected(pos))) {
        if (owns(pos)) ((GrapeVineBlockEntity) level.getBlockEntity(pos)).restoreSupport();
        branches.remove(pos);
        setChanged();
      }
    }
  }

  /** Called only from the crop's natural random tick. Bone meal never reaches this operation. */
  public boolean naturalSpread(RandomSource random) {
    if (!(level instanceof ServerLevel) || !isRoot()) return false;
    prune();
    if (getBlockState().getValue(GrapeCropBlock.AGE) < 2
        || sectionCount() >= VineyardConfig.maxTrellises()
        || level.getRawBrightness(worldPosition.above(), 0) < 9
        || random.nextInt(8) != 0) return false;
    Direction across = getBlockState().getValue(GrapeCropBlock.FACING).getClockWise();
    Direction first = random.nextBoolean() ? across : across.getOpposite();
    return extend(first) || extend(first.getOpposite());
  }

  private boolean extend(Direction direction) {
    for (int step = 1; step < VineyardConfig.MAX_SECTIONS; step++) {
      BlockPos pos = worldPosition.relative(direction, step);
      if (!loaded(level, pos)) return false;
      if (branches.contains(pos) && owns(pos)) continue;
      BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
      Direction facing = getBlockState().getValue(GrapeCropBlock.FACING);
      if (!lower.is(ModBlocks.GRAPE_TRELLIS.get())
          || !upper.is(ModBlocks.GRAPE_TRELLIS.get())
          || lower.getValue(GrapeTrellisBlock.HALF) != DoubleBlockHalf.LOWER
          || upper.getValue(GrapeTrellisBlock.HALF) != DoubleBlockHalf.UPPER
          || lower.getValue(GrapeTrellisBlock.FACING) != facing
          || upper.getValue(GrapeTrellisBlock.FACING) != facing
          || !GrapeTrellisBlock.soil(level.getBlockState(pos.below()))) return false;
      BlockState grown =
          getBlockState()
              .setValue(GrapeCropBlock.AGE, 2)
              .setValue(GrapeCropBlock.BUNCHES, 3)
              .setValue(GrapeCropBlock.BRANCH, true);
      level.setBlock(pos, grown, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
      level.setBlock(
          pos.above(),
          grown.setValue(GrapeCropBlock.HALF, DoubleBlockHalf.UPPER),
          Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
      if (!(level.getBlockEntity(pos) instanceof GrapeVineBlockEntity branch))
        throw new IllegalStateException("Missing grape ownership after spread");
      branch.origin = worldPosition.immutable();
      branch.vineId = vineId;
      branch.setChanged();
      branches.add(pos.immutable());
      setChanged();
      level.updateNeighborsAt(pos, grown.getBlock());
      level.updateNeighborsAt(pos.above(), grown.getBlock());
      return true;
    }
    return false;
  }

  private void restoreSupport() {
    if (level == null || level.isClientSide || retiring) return;
    retiring = true;
    BlockState state = getBlockState();
    var support =
        ModBlocks.GRAPE_TRELLIS
            .get()
            .defaultBlockState()
            .setValue(GrapeTrellisBlock.FACING, state.getValue(GrapeCropBlock.FACING));
    level.setBlock(worldPosition, support, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    BlockState upper = level.getBlockState(worldPosition.above());
    if (upper.is(state.getBlock()) && upper.getValue(GrapeCropBlock.HALF) == DoubleBlockHalf.UPPER)
      level.setBlock(
          worldPosition.above(),
          support.setValue(GrapeTrellisBlock.HALF, DoubleBlockHalf.UPPER),
          Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    level.updateNeighborsAt(worldPosition, support.getBlock());
    level.updateNeighborsAt(worldPosition.above(), support.getBlock());
  }

  public void removed() {
    if (level == null || level.isClientSide || retiring) return;
    retiring = true;
    if (isRoot()) {
      for (BlockPos pos : Set.copyOf(branches))
        if (owns(pos)) ((GrapeVineBlockEntity) level.getBlockEntity(pos)).restoreSupport();
    } else {
      GrapeVineBlockEntity root = owner();
      if (root != null) {
        root.branches.remove(worldPosition);
        root.setChanged();
        root.prune();
      }
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putLong("Origin", origin.asLong());
    tag.putUUID("VineId", vineId);
    tag.putLongArray("Branches", branches.stream().mapToLong(BlockPos::asLong).toArray());
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : worldPosition;
    if (tag.hasUUID("VineId")) vineId = tag.getUUID("VineId");
    branches.clear();
    for (long packed : tag.getLongArray("Branches")) {
      BlockPos pos = BlockPos.of(packed);
      if (branches.size() < VineyardConfig.MAX_SECTIONS - 1
          && !pos.equals(worldPosition)
          && pos.getY() == worldPosition.getY()
          && pos.distManhattan(worldPosition) < VineyardConfig.MAX_SECTIONS) branches.add(pos);
    }
  }
}
