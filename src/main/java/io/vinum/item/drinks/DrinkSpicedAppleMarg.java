package io.vinum.item.drinks;

import io.vinum.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

public class DrinkSpicedAppleMarg extends DrinkItem implements IDrink {
	
	public DrinkSpicedAppleMarg(Properties properties) {
		super(properties, 1, 2, new ItemStack(ModItems.MARGARITA_GLASS.get()));
		
    }
    
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		
		if (entityLiving instanceof PlayerEntity) {
			
            entityLiving.addPotionEffect(new EffectInstance(Effects.LEVITATION, 2400));
            entityLiving.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, 12000));
            entityLiving.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, 2700));
			
		}
		
		return super.onItemUseFinish(stack, worldIn, entityLiving);
		
	}
	
}