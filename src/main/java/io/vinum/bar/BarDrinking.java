package io.vinum.bar;

import io.vinum.beverage.DrinkwareAppearance;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server-timed hold gestures. No item leaves the counter; release or target changes cancel progress. */
public final class BarDrinking {
    private record Hold(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,BlockPos pos,int slot,ItemStack snapshot,long last,int ticks,boolean finished) {}
    private static final Map<Player,Hold> HOLDS=new WeakHashMap<>();
    private BarDrinking() {}
    public static void pulse(Player player,BlockPos pos,int slot,boolean active) {
        if(player.level().isClientSide)return;
        if(!active || player.isSpectator() || !player.isAlive() || player.isShiftKeyDown()
            || !player.getMainHandItem().isEmpty() || !player.mayBuild()
            || !player.level().hasChunkAt(pos) || !player.level().mayInteract(player,pos)) {
            HOLDS.remove(player);return;
        }
        var target=BarInteractions.target(player).orElse(null);
        if(target==null || !target.hit().getBlockPos().equals(pos)
            || !(player.level().getBlockEntity(pos) instanceof BarBlockEntity bar)
            || target.slot()!=slot) {
            HOLDS.remove(player);return;
        }
        ItemStack drink=bar.bottle(slot);
        if(!DrinkwareAppearance.canDrink(drink)){HOLDS.remove(player);return;}
        long now=player.level().getGameTime();
        Hold prior=HOLDS.get(player);
        boolean same=prior!=null && prior.dimension().equals(player.level().dimension()) && prior.pos().equals(pos) && prior.slot()==slot
            && ItemStack.isSameItemSameComponents(prior.snapshot(),drink);
        if(same && (prior.finished() || prior.last()==now))return;
        int ticks=same && now-prior.last()<=3 ? prior.ticks()+1 : 1;
        boolean done=ticks>=drink.getUseDuration(player);
        if(done) {
            ItemStack empty=drink.copy().finishUsingItem(player.level(),player);
            bar.replace(slot,drink,empty);
            player.level().playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS,.6f,1);
        }
        HOLDS.put(player,new Hold(player.level().dimension(),pos.immutable(),slot,drink.copy(),now,ticks,done));
    }
}
