package io.vinum.item.drinks;

import java.util.Arrays;
import java.util.List;
import io.vinum.item.AgedBeverageItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class DrinkItem extends Item implements IDrink {
    private final int useDuration;
    private final int bacLevel;
    private final ItemStack returnedItem;
    private final MobEffectInstance[] givenPotionEffects;

    public DrinkItem(Properties properties, int useDuration, int bacLevel, ItemStack returnedItem,
                     MobEffectInstance... givenPotionEffects) {
        super(properties);
        this.useDuration = useDuration;
        this.bacLevel = bacLevel;
        this.returnedItem = returnedItem.copy();
        this.givenPotionEffects = Arrays.stream(givenPotionEffects).map(MobEffectInstance::new)
                .toArray(MobEffectInstance[]::new);
    }
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!level.isClientSide) {
            for (MobEffectInstance effect : givenPotionEffects) entity.addEffect(new MobEffectInstance(effect));
            if (entity instanceof Player player) IDrink.addBACLevel(level, player, bacLevel);
        }
        if (entity instanceof Player player && player.hasInfiniteMaterials()) return stack;
        stack.shrink(1);
        ItemStack container = returnedItem.copy();
        if (stack.isEmpty()) return container;
        // A stacked shot still produces one glass; don't lose it when more drinks remain.
        if (!level.isClientSide && !container.isEmpty()) {
            if (entity instanceof Player player) {
                if (!player.getInventory().add(container)) player.drop(container, false);
            } else {
                entity.spawnAtLocation(container);
            }
        }
        return stack;
    }
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        AgedBeverageItem.appendAgeTooltip(stack, tooltip);
    }
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) { return useDuration; }
    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
