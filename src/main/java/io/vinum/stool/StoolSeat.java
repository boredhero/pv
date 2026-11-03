package io.vinum.stool;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Invisible, nonpersistent mount. No furniture/item state is stored on the entity. */
public final class StoolSeat extends Entity {
    public StoolSeat(EntityType<? extends StoolSeat> type,Level level) {
        super(type,level); noPhysics=true; setNoGravity(true); setInvulnerable(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof Player && !isVehicle(); }
    @Override protected Vec3 getPassengerAttachmentPoint(Entity passenger,EntityDimensions dimensions,float scale) { return Vec3.ZERO; }
    @Override public void tick() {
        super.tick();
        Entity rider=getFirstPassenger();
        if(!level().isClientSide && (rider==null || rider.isRemoved() || !rider.isAlive() || rider.level()!=level()
            || !level().hasChunkAt(blockPosition())
            || !level().getBlockState(blockPosition()).is(StoolContent.BARSTOOL.get()))) {
            ejectPassengers(); discard();
        }
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        for(Direction direction:Direction.Plane.HORIZONTAL) {
            var candidate=blockPosition().relative(direction);
            if(!level().hasChunkAt(candidate)) continue;
            Vec3 safe=DismountHelper.findSafeDismountLocation(passenger.getType(),level(),candidate,true);
            if(safe!=null) return safe;
        }
        // A blocked-in stool still permits dismounting onto its own seat instead of into a wall.
        return new Vec3(getX(),blockPosition().getY()+.75,getZ());
    }
}
