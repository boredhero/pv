/*
    Project Vinum - StillRecipes.java
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
package io.vinum.tileentity.recipes;

import java.util.ArrayList;

import net.minecraft.item.ItemStack;

import io.vinum.item.PVItems;

public class StillRecipes {
	
	private static final ArrayList<StillRecipe> STILL_RECIPES = new ArrayList<StillRecipe>();
	
	public void initRecipes() {
		//Add recipes for the Still here		
		addStillRecipe(new ItemStack(PVItems.FERMENTED_AGAVE_WORT.get()), new ItemStack(PVItems.FIFTH_BOTTLE_EMPTY.get()), new ItemStack(PVItems.FIFTH_SILVER_TEQUILA.get()), 240);
	}
	
	public void addStillRecipe(ItemStack distilledItemstack, ItemStack requiredBottleItemstack, ItemStack distillingResultItemstack, int ticksToDistill) {
		
		STILL_RECIPES.add(new StillRecipe(distilledItemstack, requiredBottleItemstack, distillingResultItemstack, ticksToDistill));
		
	}
	
	public ArrayList<StillRecipe> getRecipes() {
		
		return STILL_RECIPES;
		
	}
	
	public class StillRecipe {
		
		private final int ticksToDistill;
		
		private String recipeName;
		
		private final ItemStack distilledItemstack;
		private final ItemStack requiredBottleItemstack;
		private final ItemStack distillingResultItemstack;
		
		protected StillRecipe(ItemStack distilledItemstack, ItemStack requiredBottleItemstack, ItemStack distillingResultItemstack, int ticksToDistill) {
			
			this.ticksToDistill = ticksToDistill;
			this.distilledItemstack = distilledItemstack;
			this.requiredBottleItemstack = requiredBottleItemstack;
			this.distillingResultItemstack = distillingResultItemstack;
			
		}
		
		protected StillRecipe(ItemStack distilledItemstack, ItemStack requiredBottleItemstack, ItemStack distillingResultItemstack, int ticksToDistill, String recipeName) {
			
			this.ticksToDistill = ticksToDistill;
			this.recipeName = recipeName;
			this.distilledItemstack = distilledItemstack;
			this.requiredBottleItemstack = requiredBottleItemstack;
			this.distillingResultItemstack = distillingResultItemstack;
			
		}
		
		public int getTicksToDistill() {
			
			return ticksToDistill;
			
		}
		
		public String getRecipeName() {
			
			if (recipeName != null) {
				
				return recipeName;
				
			} else {
				
				return "Unnamed Recipe";
				
			}
			
		}
		
		public ItemStack getDistilledItemstack() {
			
			return distilledItemstack;
			
		}
		
		public ItemStack getRequiredBottleItemstack() {
			
			return requiredBottleItemstack;
			
		}
		
		public ItemStack getDistillingResultItemstack() {
			
			return distillingResultItemstack;
			
		}
		
		public boolean matchesRecipe(ItemStack distilledItemstack, ItemStack requiredBottleItemstack) {
			
			if (distilledItemstack != null && distilledItemstack != null) {

				return distilledItemstack.getItem().equals(this.distilledItemstack.getItem()) || requiredBottleItemstack.getItem().equals(this.requiredBottleItemstack.getItem());
				
			}
			
			return false;
			
		}
		
	}
	
}