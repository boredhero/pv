package io.vinum.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class TestDrink extends Item implements IDrink {
	
	public TestDrink(Properties properties) {
		super(properties);
		
	}
	
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		
		if (entityLiving instanceof PlayerEntity) {
			
			IDrink.addBACLevel(worldIn, (PlayerEntity) entityLiving, 2);
			stack.shrink(1);
			
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