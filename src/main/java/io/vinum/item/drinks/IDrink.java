package io.vinum.item.drinks;

import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.common.Defines;
import io.vinum.network.NetworkLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Public entry points for other drinks to participate in Vinum's alcohol system. */
public interface IDrink {
    ResourceKey<DamageType> ALCOHOL_POISONING = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Defines.MODID, "too_drunk"));

    static void setBACLevel(Level world, Player player, int level) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BAC state = player.getData(BACCapability.BAC_STATE);
            int previous = state.getBACLevel();
            state.setBACLevel(level);
            if (state.getBACLevel() != previous) NetworkLoader.sync(serverPlayer);
            updatePlayerBAC(player, state.getBACLevel());
        }
    }
    static void addBACLevel(Level world, Player player, int amount) {
        int current = player.getData(BACCapability.BAC_STATE).getBACLevel();
        setBACLevel(world, player, (int) Math.clamp((long) current + amount, 0, Integer.MAX_VALUE));
    }
    static void removeBACLevel(Level world, Player player, int amount) {
        int current = player.getData(BACCapability.BAC_STATE).getBACLevel();
        setBACLevel(world, player, (int) Math.clamp((long) current - amount, 0, Integer.MAX_VALUE));
    }
    static void updatePlayerBAC(Player player, int level) {
        if (player.level().isClientSide || player.isCreative() || player.isSpectator() || level <= 0) return;
        if (level >= 10) {
            player.hurt(new DamageSource(player.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(ALCOHOL_POISONING)), 40.0F);
            player.removeAllEffects();
            return;
        }
        sustainEffect(player, MobEffects.REGENERATION, 605);
        if (level >= 2) sustainEffect(player, MobEffects.DAMAGE_BOOST, 605);
        if (level >= 3) sustainEffect(player, MobEffects.JUMP, 605);
        if (level >= 4) sustainEffect(player, MobEffects.ABSORPTION, 605);
        if (level >= 6) sustainEffect(player, MobEffects.CONFUSION, 1005);
        if (level >= 7) sustainEffect(player, MobEffects.WEAKNESS, 1805);
        if (level >= 8) sustainEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1405);
        if (level >= 9) sustainEffect(player, MobEffects.BLINDNESS, 605);
    }

    private static void sustainEffect(Player player, Holder<MobEffect> effect, int duration) {
        MobEffectInstance existing = player.getEffect(effect);
        // Let effects count down normally: resetting regeneration every tick prevents its pulses.
        // A 200-tick refresh interval is divisible by regeneration's 50-tick period.
        if (existing == null || (!existing.isInfiniteDuration() && existing.getDuration() <= duration - 200)) {
            player.addEffect(new MobEffectInstance(effect, duration));
        }
    }
}
