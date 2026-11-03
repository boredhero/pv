package io.vinum.bar;

import java.util.HashSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Display shapes occupy the cell above their owning bar, which vanilla block picking can skip. */
public final class BarInteractions {
    public record Target(BlockHitResult hit,int slot) {}
    private BarInteractions() {}
    public static Optional<Target> target(Player player) {
        double reach=Math.min(64,player.blockInteractionRange());
        Vec3 from=player.getEyePosition(),to=from.add(player.calculateViewVector(player.getXRot(),player.getYRot()).scale(reach));
        var level=player.level();
        var candidates=new HashSet<BlockPos>();
        BlockGetter.traverseBlocks(from,to,candidates,(positions,pos)->{
            positions.add(pos.immutable());positions.add(pos.below());return null;
        },positions->null);
        // Check the whole ray before querying block states, including the occlusion trace.
        if(candidates.stream().anyMatch(pos->!level.hasChunkAt(pos)))return Optional.empty();
        var obstruction=level.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        double nearest=obstruction.getType()==HitResult.Type.MISS?reach*reach:from.distanceToSqr(obstruction.getLocation())+1.0e-7;
        Target best=null;
        for(BlockPos pos:candidates) {
            if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof BarBlockEntity bar))continue;
            for(int slot=0;slot<BarBlockEntity.SLOTS;slot++) {
                var hit=BarBlock.displayShape(bar.getBlockState(),bar,slot).clip(from,to,pos);
                if(hit==null)continue;
                double distance=from.distanceToSqr(hit.getLocation());
                if(distance<=nearest){nearest=distance;best=new Target(hit,slot);}
            }
        }
        return Optional.ofNullable(best);
    }
    /** Packet carries intent only; aim, reach, obstruction, inventory and permissions are server-owned. */
    public static void use(Player player,InteractionHand hand) {
        if(player.level().isClientSide||player.isSpectator()||!player.isAlive()||!player.mayBuild())return;
        var target=target(player).orElse(null);
        if(target==null)return;
        var pos=target.hit().getBlockPos();
        if(!player.level().mayInteract(player,pos)||!player.mayUseItemAt(pos,target.hit().getDirection(),player.getItemInHand(hand)))return;
        var state=player.level().getBlockState(pos);
        var result=state.useItemOn(player.getItemInHand(hand),player.level(),player,hand,target.hit());
        if(result==ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION&&hand==InteractionHand.MAIN_HAND)
            state.useWithoutItem(player.level(),player,target.hit());
    }
}
