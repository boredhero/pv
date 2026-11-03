package io.vinum.stool;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** The stool is the persistent furniture; its seat exists only while a player is using it. */
public final class BarstoolBlock extends Block {
    public static final MapCodec<BarstoolBlock> CODEC = simpleCodec(BarstoolBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(2,10,2,14,12,14),
        box(3,0,3,5,10,5),box(11,0,3,13,10,5),box(3,0,11,5,10,13),box(11,0,11,13,10,13),
        box(4,3,4,12,4,5),box(4,3,11,12,4,12),box(4,3,4,5,4,12),box(11,3,4,12,4,12));
    public BarstoolBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if(player.isShiftKeyDown() || player.isSpectator() || player.isPassenger() || !player.isAlive() || !level.mayInteract(player,pos))
            return InteractionResult.PASS;
        var seats=level.getEntitiesOfClass(StoolSeat.class,new AABB(pos).inflate(.1),seat -> seat.blockPosition().equals(pos));
        if(seats.stream().anyMatch(seat -> seat.isVehicle())) return InteractionResult.FAIL;
        if(!level.noCollision(player,new AABB(pos.getX()+.15,pos.getY()+.76,pos.getZ()+.15,pos.getX()+.85,pos.getY()+2,pos.getZ()+.85)))
            return InteractionResult.FAIL;
        if(level.isClientSide) return InteractionResult.SUCCESS;
        seats.forEach(StoolSeat::discard);
        StoolSeat seat=new StoolSeat(StoolContent.SEAT.get(),level);
        seat.setPos(pos.getX()+.5,pos.getY()+.75,pos.getZ()+.5);
        if(!level.addFreshEntity(seat)) return InteractionResult.FAIL;
        if(!player.startRiding(seat,true)) { seat.discard(); return InteractionResult.FAIL; }
        seat.positionRider(player);
        return InteractionResult.CONSUME;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving) {
        if(!replacement.is(this) && !level.isClientSide)
            for(var seat:level.getEntitiesOfClass(StoolSeat.class,new AABB(pos).inflate(.1),seat -> seat.blockPosition().equals(pos))) {
                seat.ejectPassengers(); seat.discard();
            }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
