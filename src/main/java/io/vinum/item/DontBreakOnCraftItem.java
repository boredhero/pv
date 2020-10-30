/*
    Project Vinum - DontBreakOnCraftItem.java
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