package io.vinum.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * This class exsits so that items like juicer and ice_pick can return themselves as a container when crafted.
 * It is hacky, and not elegant. But the alternative is implementing a custom crafting handler or using Reflection
 * to change the Item.Properties of an item made and registered like the rest of the items via DeferredRegister.
 * These solutions both struck me as inelegant in their own way.
 * And so, LONG LIVE OLD FASHIONED REGISTRY EVENTS!!! DeferredRegister is henceforth....deferred...
 * 
 * ^^^
 * Sounds like one of drunk Rick's ramblings.
 * 
 */

public class DontBreakOnCraftItem extends Item {
	
	public DontBreakOnCraftItem(Item.Properties builder) {
		super(builder);
		
	}
	
    @Override
    public ItemStack getContainerItem(ItemStack stack) {
    	
    	return stack.copy();
    	
    }
    
    @Override
    public boolean hasContainerItem(ItemStack stack) {
    	
    	return true;
    	
    }
    
}