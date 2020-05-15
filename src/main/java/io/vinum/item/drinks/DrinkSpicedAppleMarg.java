package io.vinum.item.drinks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class DrinkSpicedAppleMarg extends Item implements IDrink {
	
	public DrinkSpicedAppleMarg(Properties properties) {
		super(properties);
		
    }
    
    public EffectInstance levitation_5m = new EffectInstance(Effects.LEVITATION, 6000);
    public EffectInstance jump_boost_10m = new EffectInstance(Effects.JUMP_BOOST, 12000);
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		
		if (entityLiving instanceof PlayerEntity) {
			
			IDrink.addBACLevel(worldIn, (PlayerEntity) entityLiving, 2);
            stack.shrink(1);
            entityLiving.addPotionEffect(levitation_5m);
            entityLiving.addPotionEffect(jump_boost_10m);
			
		}
		
		return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
		
	}
	
	@Override
	public int getUseDuration(ItemStack stack) {
		
		return 14;
		
	}
	
	@Override
	public UseAction getUseAction(ItemStack stack) {
		
		return UseAction.DRINK;
		
	}
	
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		
		playerIn.setActiveHand(handIn);
		
		return ActionResult.resultSuccess(playerIn.getHeldItem(handIn));
		
	}
	
}