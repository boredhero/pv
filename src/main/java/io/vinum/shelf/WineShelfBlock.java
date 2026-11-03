package io.vinum.shelf;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;

/** Matching facing shelves share their outside trim; each block remains an independent nine-bottle rack. */
public final class WineShelfBlock extends Block implements EntityBlock {
  public static final MapCodec<WineShelfBlock> CODEC = simpleCodec(WineShelfBlock::new);
  public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
  public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right"), UP = BooleanProperty.create("up"), DOWN = BooleanProperty.create("down");
  // Keep the original five slot IDs at their nearest positions; old saves need no inventory rewrite.
  public static final double[][] CENTERS = {{.5,.8},{.2,.5},{.8,.5},{.5,.2},{.5,.5},
      {.2,.8},{.8,.8},{.2,.2},{.8,.2}};
  public WineShelfBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LEFT,false).setValue(RIGHT,false).setValue(UP,false).setValue(DOWN,false)); }
  @Override protected MapCodec<? extends Block> codec() { return CODEC; }
  @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,LEFT,RIGHT,UP,DOWN); }
  private boolean connects(LevelReader level, BlockPos pos, Direction facing) {
    if (level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false) == null) return false;
    BlockState other = level.getBlockState(pos);
    return other.is(this) && other.getValue(FACING) == facing;
  }
  private BlockState connected(BlockState state, LevelReader level, BlockPos pos) {
    Direction face=state.getValue(FACING);
    return state.setValue(LEFT,connects(level,pos.relative(face.getCounterClockWise()),face)).setValue(RIGHT,connects(level,pos.relative(face.getClockWise()),face)).setValue(UP,connects(level,pos.above(),face)).setValue(DOWN,connects(level,pos.below(),face));
  }
  @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return connected(defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()),context.getLevel(),context.getClickedPos()); }
  @Override protected BlockState updateShape(BlockState state, Direction side, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) { return connected(state, level, pos); }
  @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WineShelfBlockEntity(pos,state); }
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    return level.isClientSide || type != ShelfContent.ENTITY.get() ? null : (world,pos,block,entity) -> WineShelfBlockEntity.tick(world,pos,block,(WineShelfBlockEntity)entity);
  }
  public static int slot(BlockState state, BlockPos pos, BlockHitResult hit) {
    double x=hit.getLocation().x-pos.getX(), z=hit.getLocation().z-pos.getZ(), y=hit.getLocation().y-pos.getY();
    double across=switch(state.getValue(FACING)){case NORTH->x;case EAST->z;case SOUTH->1-x;case WEST->1-z;default->x;};
    int best=0; double distance=Double.MAX_VALUE;
    for(int index=0;index<CENTERS.length;index++){double dx=across-CENTERS[index][0],dy=y-CENTERS[index][1],d=dx*dx+dy*dy;if(d<distance){best=index;distance=d;}}
    return best;
  }
  @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    if (!WineShelfBlockEntity.accepts(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    if (hit.getDirection()!=state.getValue(FACING) || !player.mayBuild() || !level.mayInteract(player,pos)) return ItemInteractionResult.FAIL;
    if (!(level.getBlockEntity(pos) instanceof WineShelfBlockEntity shelf)) return ItemInteractionResult.FAIL;
    int slot=slot(state,pos,hit);
    if (!shelf.bottle(slot).isEmpty()) return ItemInteractionResult.FAIL;
    if (!level.isClientSide && shelf.insert(slot,stack) && !player.hasInfiniteMaterials()) stack.shrink(1);
    return ItemInteractionResult.sidedSuccess(level.isClientSide);
  }
  @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (hit.getDirection()!=state.getValue(FACING) || !player.mayBuild() || !level.mayInteract(player,pos)) return InteractionResult.PASS;
    if (!(level.getBlockEntity(pos) instanceof WineShelfBlockEntity shelf)) return InteractionResult.PASS;
    int slot=slot(state,pos,hit); if(shelf.bottle(slot).isEmpty()) return InteractionResult.PASS;
    if(!level.isClientSide){ItemStack bottle=shelf.remove(slot);if(!player.getInventory().add(bottle)) player.drop(bottle,false);}
    return InteractionResult.sidedSuccess(level.isClientSide);
  }
  @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if(!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof WineShelfBlockEntity shelf)
      for(int slot=0;slot<WineShelfBlockEntity.SLOTS;slot++) Block.popResource(level,pos,shelf.remove(slot));
    super.onRemove(state,level,pos,replacement,moving);
  }
  @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
  @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
  @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 20; }
  @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
}
