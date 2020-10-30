/*
    Project Vinum - IDrink.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package io.vinum.item.drinks;

import javax.annotation.Nonnull;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraftforge.common.util.NonNullConsumer;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import io.vinum.common.PVDefines;

public interface IDrink {
	
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving);
	public int getUseDuration(ItemStack stack);
	public UseAction getUseAction(ItemStack stack);
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn);
	
	public static void setBACLevel(World world, PlayerEntity player, double level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.setBACLevel(level);
					updatePlayerBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}

public static void addBACLevel(World world, PlayerEntity player, double level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.addBACLevel(level);
					updatePlayerBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	
	public static void removeBACLevel(World world, PlayerEntity player, double level) {
		
		if (!world.isRemote()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					iBAC.removeBACLevel(level);
					updatePlayerBAC(player, iBAC.getBACLevel());
					
				}
				
			});
			
		}
		
	}
	
	public static void updatePlayerBAC(PlayerEntity player, double level) {
		
		if (!player.isCreative() && level > 0) {
			
			if (level >= 0.001) {
				
				player.addPotionEffect(new EffectInstance(Effects.REGENERATION, 605));
				
			}
			
			if (level >= 2) {
				
				player.addPotionEffect(new EffectInstance(Effects.STRENGTH, 605));
				
			}
			
			if (level >= 3) {
				
				player.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, 605));
				
			}
			
			if (level >= 4) {
				
				player.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 605));
				
			}
			
			if (level >= 6) {
				
				player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 1005));
				
			}
			
			if (level >= 7) {
				
				player.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 1805));
				
			}
			
			if (level >= 8) {
				
				player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 1405));
				
			}
			
			if (level >= 9) {
				
				player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 605));
			}
			
			if (level >= 10) {
				
				player.attackEntityFrom(new DamageSource(PVDefines.MODID + ".too_drunk"), 40F);
				player.clearActivePotions();
				
			}
			
		}
		
	}
	
}