package io.vinum.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server-owned alcohol state; the persisted decay timer is never sent each tick. */
public final class BAC implements IBAC, INBTSerializable<CompoundTag> {
    public static final int DECAY_TICKS = 4000;
    private int level;
    private int ticks;
    private double alcoholRemainder;

    /** Returns whole gameplay BAC points; retains fractional servings across drinking and saves. */
    public int acceptAlcohol(double millibuckets) {
        if (!Double.isFinite(millibuckets) || millibuckets < 0) throw new IllegalArgumentException("Invalid alcohol dose");
        double points = alcoholRemainder + millibuckets / 15.0;
        int whole = (int)Math.min(Integer.MAX_VALUE, Math.floor(points + 1e-9));
        alcoholRemainder = Math.max(0, points - whole);
        return whole;
    }

    @Override
    public int getBACLevel() { return level; }
    @Override
    public void setBACLevel(int level) {
        this.level = Math.clamp(level, 0, Integer.MAX_VALUE);
        if (this.level == 0) ticks = 0;
    }
    @Override
    public void addBACLevel(int amount) { setBACLevel((int) Math.clamp((long) level + amount, 0, Integer.MAX_VALUE)); }
    @Override
    public void removeBACLevel(int amount) { setBACLevel((int) Math.clamp((long) level - amount, 0, Integer.MAX_VALUE)); }
    @Override
    public int getBACTicks() { return ticks; }
    @Override
    public void setBACTicks(int ticks) { this.ticks = Math.clamp(ticks, 0, DECAY_TICKS); }
    @Override
    public void addBACTicks(int amount) { setBACTicks((int) Math.clamp((long) ticks + amount, 0, DECAY_TICKS)); }
    @Override
    public void removeBACTicks(int amount) { setBACTicks((int) Math.clamp((long) ticks - amount, 0, DECAY_TICKS)); }

    /** @return whether the visible level changed. */
    public boolean tick() {
        if (level == 0) {
            ticks = 0;
            return false;
        }
        if (++ticks >= DECAY_TICKS) {
            ticks = 0;
            --level;
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BACLevel", level);
        tag.putInt("BACTicks", ticks);
        tag.putDouble("AlcoholRemainder", alcoholRemainder);
        return tag;
    }
    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        double remainder = tag.getDouble("AlcoholRemainder");
        alcoholRemainder = Double.isFinite(remainder) && remainder >= 0 && remainder < 1 ? remainder : 0;
        setBACLevel(tag.getInt("BACLevel"));
        setBACTicks(level == 0 ? 0 : tag.getInt("BACTicks"));
    }
}
