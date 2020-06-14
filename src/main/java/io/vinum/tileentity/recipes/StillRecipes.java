package io.vinum.tileentity.recipes;

import java.util.ArrayList;

import io.vinum.item.ModItems;
import net.minecraft.item.ItemStack;

public class StillRecipes {
	
	private static final ArrayList<StillRecipe> STILL_RECIPES = new ArrayList<StillRecipe>();
	
	public void initRecipes() {
		
		addStillRecipe(new ItemStack(ModItems.FERMENTED_AGAVE_WORT.get()), new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()), new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get()), 240);
		addStillRecipe(new ItemStack(ModItems.APPLE_JUICE.get()), new ItemStack(ModItems.BARREL.get()), new ItemStack(ModItems.STEEL_INGOT.get()), 25);
		
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
				
				if (distilledItemstack.getItem().equals(this.distilledItemstack.getItem()) || requiredBottleItemstack.getItem().equals(this.requiredBottleItemstack.getItem())) {
					
					return true;
					
				}
				
			}
			
			return false;
			
		}
		
	}
	
}