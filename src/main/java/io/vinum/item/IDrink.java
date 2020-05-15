package io.vinum.item;

import javax.annotation.Nonnull;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.util.NonNullConsumer;

public interface IDrink {
	
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving);
	public int getUseDuration(ItemStack stack);
	public UseAction getUseAction(ItemStack stack);
	
	public static void setBACLevel(World world, PlayerEntity player, int level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.setBACLevel(level);
					
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
					
				}
				
			});
			
		}
		
	}
	
	public static void killPlayerFromBAC(PlayerEntity player, int level) {
		
		if (level >= 10) {
			
			player.onDeath(new DamageSource("too_drunk"));
			
		}
		
	}
	
}