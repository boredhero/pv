package io.vinum.item.drinks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class DrinkItem extends Item implements IDrink {
	
	private int useDuration;
	private int BACLevel;
	private ItemStack returnedItem;
	private EffectInstance[] givenPotionEffects;
	
	public DrinkItem(Properties properties, int useDuration, int BACLevel, ItemStack returnedItem) {
		super(properties);
		
		this.useDuration = useDuration;
		this.BACLevel = BACLevel;
		this.returnedItem = returnedItem;
		this.givenPotionEffects = new EffectInstance[0];
		
	}
	
	public DrinkItem(Properties properties, int useDuration, int BACLevel, ItemStack returnedItem, EffectInstance... givenPotionEffects) {
		super(properties);
		
		this.useDuration = useDuration;
		this.BACLevel = BACLevel;
		this.returnedItem = returnedItem;
		this.givenPotionEffects = givenPotionEffects;
		
	}
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		
		if (!worldIn.isRemote()) {
			
			if (entityLiving instanceof PlayerEntity) {
				
				for (EffectInstance potionEffect : givenPotionEffects) {
					
					entityLiving.addPotionEffect(potionEffect);
					
				}
				
				IDrink.addBACLevel(worldIn, (PlayerEntity) entityLiving, BACLevel);
				stack.shrink(1);
				
			}
			
		}
		
		return stack.isEmpty() ? returnedItem : stack;
		
	}
	
	@Override
	public int getUseDuration(ItemStack stack) {
		
		return useDuration;
		
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