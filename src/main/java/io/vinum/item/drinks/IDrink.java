package io.vinum.item.drinks;

import javax.annotation.Nonnull;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import io.vinum.common.Defines;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
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
					killPlayerFromBAC(player, iBAC.getBACLevel());
					
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
					killPlayerFromBAC(player, iBAC.getBACLevel());
					
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
					killPlayerFromBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	
	public static void killPlayerFromBAC(PlayerEntity player, int level) {
		
		System.out.println(player);
		System.out.println(level);
		
		if (level >= 10 && !player.isCreative()) {
			
			player.attackEntityFrom(new DamageSource(Defines.MODID + ".too_drunk"), 40F);
			
		}
		
	}
	
}