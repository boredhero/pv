package io.vinum.bar;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Five individually targeted positions in a staggered counter layout. */
public final class BarBlock extends Block implements EntityBlock {
    public static final MapCodec<BarBlock> CODEC = simpleCodec(BarBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final double[][] CENTERS = {{.25,.25},{.75,.25},{.5,.5},{.25,.75},{.75,.75}};
    private static VoxelShape counterShape(BlockState state) {
        double lo=state.getValue(LEFT)?0:1,hi=state.getValue(RIGHT)?16:15;
        VoxelShape body=switch(state.getValue(FACING)) {
            case EAST -> box(1,0,lo,14,14,hi);
            case SOUTH -> box(16-hi,0,1,16-lo,14,14);
            case WEST -> box(2,0,16-hi,15,14,16-lo);
            default -> box(lo,0,2,hi,14,15);
        };
        return Shapes.or(box(0,14,0,16,16,16),body);
    }
    public BarBlock(Properties properties) { super(properties.dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT,false).setValue(RIGHT,false)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING,LEFT,RIGHT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return connections(defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),context.getLevel(),context.getClickedPos()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BarBlockEntity(pos, state); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return counterShape(state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape outline = counterShape(state);
        if (level.getBlockEntity(pos) instanceof BarBlockEntity bar) for (int i=0;i<BarBlockEntity.SLOTS;i++) {
            if (bar.bottle(i).isEmpty()) continue;
            double[] center = center(state,i);
            double x=center[0],z=center[1];
            boolean alongX=state.getValue(FACING).getAxis()==Direction.Axis.Z;
            double rx=alongX?.14:.09,rz=alongX?.09:.14;
            outline=Shapes.or(outline,Shapes.box(x-rx,1,z-rz,x+rx,1.58,z+rz));
        }
        return outline;
    }
    public static double[] center(BlockState state,int slot) {
        double x=CENTERS[slot][0],z=CENTERS[slot][1];
        return switch(state.getValue(FACING)) {
            case EAST -> new double[]{1-z,x};
            case SOUTH -> new double[]{1-x,1-z};
            case WEST -> new double[]{z,1-x};
            default -> new double[]{x,z};
        };
    }
    public static int slot(BlockState state, BlockPos pos, BlockHitResult hit) {
        double x=hit.getLocation().x-pos.getX(),z=hit.getLocation().z-pos.getZ();
        int best=0; double distance=Double.POSITIVE_INFINITY;
        for(int i=0;i<CENTERS.length;i++) {
            double[] center=center(state,i);
            double d=(x-center[0])*(x-center[0])+(z-center[1])*(z-center[1]);
            if(d<distance){best=i;distance=d;}
        }
        return best;
    }
    private BlockState connections(BlockState state,LevelAccessor level,BlockPos pos) {
        Direction facing=state.getValue(FACING);
        return state.setValue(LEFT,level.hasChunkAt(pos.relative(facing.getCounterClockWise())) && connects(level.getBlockState(pos.relative(facing.getCounterClockWise())),facing))
            .setValue(RIGHT,level.hasChunkAt(pos.relative(facing.getClockWise())) && connects(level.getBlockState(pos.relative(facing.getClockWise())),facing));
    }
    private boolean connects(BlockState neighbor,Direction facing) { return neighbor.is(this) && neighbor.getValue(FACING)==facing; }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return connections(state,level,pos);
    }
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        BlockState joined=connections(state,level,pos);
        if(joined!=state) level.setBlockAndUpdate(pos,joined);
    }
    private boolean usable(Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hit.getDirection() != Direction.DOWN && player.mayBuild() && level.mayInteract(player, pos);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!BarBlockEntity.accepts(held)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!usable(level,pos,player,hit) || !player.mayUseItemAt(pos,hit.getDirection(),held)
                || !(level.getBlockEntity(pos) instanceof BarBlockEntity bar)) return ItemInteractionResult.FAIL;
        int slot = slot(state,pos,hit);
        if (!bar.bottle(slot).isEmpty()) return ItemInteractionResult.FAIL;
        if (!level.isClientSide && bar.insert(slot,held) && !player.hasInfiniteMaterials()) held.shrink(1);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!usable(level,pos,player,hit) || !(level.getBlockEntity(pos) instanceof BarBlockEntity bar)) return InteractionResult.PASS;
        int slot = slot(state,pos,hit);
        if (bar.bottle(slot).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) { ItemStack bottle = bar.remove(slot); if (!player.getInventory().add(bottle)) player.drop(bottle,false); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) && !level.isClientSide && level.getBlockEntity(pos) instanceof BarBlockEntity bar)
            for (int slot = 0; slot < BarBlockEntity.SLOTS; slot++) popResource(level,pos,bar.remove(slot));
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { BlockState result=rotate(state,mirror.getRotation(state.getValue(FACING)));
        return mirror==Mirror.NONE?result:result.setValue(LEFT,state.getValue(RIGHT)).setValue(RIGHT,state.getValue(LEFT)); }
}
