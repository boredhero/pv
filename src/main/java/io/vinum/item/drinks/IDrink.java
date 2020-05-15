package io.vinum.item.drinks;

import javax.annotation.Nonnull;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import io.vinum.common.Defines;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.Potions;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.NonNullConsumer;

public interface IDrink {
	
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving);
	public int getUseDuration(ItemStack stack);
	public UseAction getUseAction(ItemStack stack);
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn);
	
	public static void setBACLevel(World world, PlayerEntity player, int level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.setBACLevel(level);
					applyAffectsToPlayerFromBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	
	public static void addBACLevel(World world, PlayerEntity player, int level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.addBACLevel(level);
					applyAffectsToPlayerFromBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	
	public static void removeBACLevel(World world, PlayerEntity player, int level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.removeBACLevel(level);
					applyAffectsToPlayerFromBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	//Make some effect instances for use below!
	public EffectInstance blindness_3m = new EffectInstance(Effects.BLINDNESS, 3600);
	public EffectInstance jump_boost_1m = new EffectInstance(Effects.JUMP_BOOST, 1200);
	public EffectInstance strength_2m = new EffectInstance(Effects.STRENGTH, 2400);
	public EffectInstance health_boost_2m = new EffectInstance(Effects.HEALTH_BOOST, 2400);
	public EffectInstance nausea_5m = new EffectInstance(Effects.NAUSEA, 6000);
	public EffectInstance absorption_3m = new EffectInstance(Effects.ABSORPTION, 3600);
	
	public static void applyAffectsToPlayerFromBAC(PlayerEntity player, int level) {
		
		System.out.println(player);
		System.out.println(level);

		if(level >= 1 && !player.isCreative()){
			player.addPotionEffect(health_boost_2m);
		}
		if(level >= 2 && !player.isCreative()){
			player.addPotionEffect(strength_2m);
		}
		if(level >= 3 && !player.isCreative()){
			player.addPotionEffect(jump_boost_1m);
		}
		if(level >= 4 && !player.isCreative()){
			player.addPotionEffect(absorption_3m);
		}
		//5 is a neutral point, do nothing.
		if(level >= 6 && !player.isCreative()){
			player.addPotionEffect(nausea_5m)
		}
		if(level >= 7 && !player.isCreative()){
			player.addPotionEffect(Potions.LONG_WEAKNESS.getEffects().get(0));
		}
		if(level >= 8 && !player.isCreative()){
			player.addPotionEffect(Potions.LONG_WEAKNESS.getEffects().get(0));
			player.addPotionEffect(Potions.LONG_SLOWNESS.getEffects().get(0));
		}
		if(level >= 9 && !player.isCreative()){
			player.addPotionEffect(Potions.LONG_POISON.getEffects().get(0));
			player.addPotionEffect(blindness_3m);
		}
		if (level >= 10 && !player.isCreative()) {
			player.attackEntityFrom(new DamageSource(Defines.MODID + ".too_drunk"), 40F);	
		}
		
	}
	//Not sure if we'll ever need this but it seems like a helpful method to have here.
	public static void forceApplyEffectToPlayer(PlayerEntity player, EffectInstance effectIn){
		player.addPotionEffect(effectIn);
	}
	
}