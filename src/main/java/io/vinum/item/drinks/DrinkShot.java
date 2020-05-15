package io.vinum.item.drinks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

<<<<<<< HEAD:src/main/java/io/vinum/item/drinks/DrinkItem.java
public class DrinkItem extends Item implements IDrink {
	
	private int useDuration;
	private int BACLevel;
	private ItemStack returnedItem;
	
	public DrinkItem(Properties properties, int useDuration, int BACLevel, ItemStack returnedItem) {
		super(properties);
		
		this.useDuration = useDuration;
		this.BACLevel = BACLevel;
		this.returnedItem = returnedItem;
		
	}
=======
public class DrinkShot extends Item implements IDrink {
	
	public DrinkShot(Properties properties) {
		super(properties);
		
    }
>>>>>>> 5b5a3ad480c002f0b9004e71dcc741663b6d2ba9:src/main/java/io/vinum/item/drinks/DrinkShot.java
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		
		if (!worldIn.isRemote()) {
			
<<<<<<< HEAD:src/main/java/io/vinum/item/drinks/DrinkItem.java
			if (entityLiving instanceof PlayerEntity) {
				
				IDrink.addBACLevel(worldIn, (PlayerEntity) entityLiving, BACLevel);
				stack.shrink(1);
				
			}
			
=======
			IDrink.addBACLevel(worldIn, (PlayerEntity) entityLiving, 1);
            stack.shrink(1);			
>>>>>>> 5b5a3ad480c002f0b9004e71dcc741663b6d2ba9:src/main/java/io/vinum/item/drinks/DrinkShot.java
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