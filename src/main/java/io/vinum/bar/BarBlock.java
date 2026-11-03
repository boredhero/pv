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

/** Five individually targeted positions across a continuous counter top. */
public final class BarBlock extends Block implements EntityBlock {
    public static final MapCodec<BarBlock> CODEC = simpleCodec(BarBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final double[] CENTERS = {.12, .31, .5, .69, .88};
    private static final VoxelShape SHAPE = Shapes.or(box(0, 14, 0, 16, 16, 16), box(1, 0, 2, 15, 14, 15));
    public BarBlock(Properties properties) { super(properties.dynamicShape().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BarBlockEntity(pos, state); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape outline = SHAPE;
        if (level.getBlockEntity(pos) instanceof BarBlockEntity bar) for (int i=0;i<BarBlockEntity.SLOTS;i++) {
            if (bar.bottle(i).isEmpty()) continue;
            double a=CENTERS[i], x=a,z=.53;
            switch(state.getValue(FACING)) {
                case EAST -> { x=.47; z=a; }
                case SOUTH -> { x=1-a; z=.47; }
                case WEST -> { x=.53; z=1-a; }
                default -> {}
            }
            outline=Shapes.or(outline,Shapes.box(x-.085,1,z-.085,x+.085,1.58,z+.085));
        }
        return outline;
    }
    public static int slot(BlockState state, BlockPos pos, BlockHitResult hit) {
        double x = hit.getLocation().x - pos.getX(), z = hit.getLocation().z - pos.getZ();
        double across = switch (state.getValue(FACING)) { case NORTH -> x; case EAST -> z; case SOUTH -> 1-x; case WEST -> 1-z; default -> x; };
        int best = 0;
        for (int i = 1; i < CENTERS.length; i++) if (Math.abs(across-CENTERS[i]) < Math.abs(across-CENTERS[best])) best = i;
        return best;
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
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return rotate(state,mirror.getRotation(state.getValue(FACING))); }
}
