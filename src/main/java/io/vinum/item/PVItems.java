/*
    Project Vinum - PVItems.java
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

import net.minecraft.item.BlockNamedItem;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

import io.vinum.item.drinks.DrinkItem;
import io.vinum.block.PVBlocks;
import io.vinum.common.PVDefines;

public final class PVItems {

	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, PVDefines.MODID);

	//Metals
	public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(PVItemGroup.PROPERTIES));
	//Agave Products
	public static final RegistryObject<Item> UNFERMENTED_AGAVE_WORT = ITEMS.register("unfermented_agave_wort", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FERMENTED_AGAVE_WORT = ITEMS.register("fermented_agave_wort", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> AGAVE_SEEDS = ITEMS.register("agave_seeds", () -> new BlockNamedItem(PVBlocks.CROP_AGAVE.get(), (PVItemGroup.PROPERTIES)));
	public static final RegistryObject<Item> AGAVE = ITEMS.register("agave", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> COOKED_AGAVE_PULP = ITEMS.register("cooked_agave_pulp", () -> new Item(PVItemGroup.PROPERTIES.food(Foods.DRIED_KELP)));
	//Glassware and glass crafting
	public static final RegistryObject<Item> MOLTEN_GLASS = ITEMS.register("molten_glass", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FIFTH_BOTTLE_EMPTY = ITEMS.register("fifth_bottle_empty", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> PACK_OF_SHOT_GLASSES = ITEMS.register("pack_of_shot_glasses", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> MARGARITA_GLASS = ITEMS.register("margarita_glass", () -> new Item(PVItemGroup.PROPERTIES));
	//Fifths of Booze
	public static final RegistryObject<Item> FIFTH_SILVER_TEQUILA = ITEMS.register("fifth_silver_tequila", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FIFTH_GOLDEN_TEQUILA = ITEMS.register("fifth_golden_tequila", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FIFTH_RECTIFIED_SPIRITS = ITEMS.register("fifth_rectified_spirits", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FIFTH_VODKA = ITEMS.register("fifth_vodka", () -> new Item(PVItemGroup.PROPERTIES));
	//public static final RegistryObject<Item> FIFTH_RECTIFIED_SPIRIT = ITEMS.register("fifth_rectified_spirits", () -> new Item(ModItemGroup.PROPERTIES));
	//Items used in crafting that must not break when used.
	public static final RegistryObject<Item> JUICER = ITEMS.register("juicer", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(PVItemGroup.MAIN)));
	public static final RegistryObject<Item> ICE_PICK = ITEMS.register("ice_pick", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(PVItemGroup.MAIN)));
	public static final RegistryObject<Item> COCKTAIL_SHAKER = ITEMS.register("cocktail_shaker", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(PVItemGroup.MAIN)));
	public static final RegistryObject<Item> MARGARITA_GLASS_MOLD = ITEMS.register("margarita_glass_mold", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(PVItemGroup.MAIN)));
	//Misc Cocktail Ingredients
	public static final RegistryObject<Item> ICE_CUBES = ITEMS.register("ice_cubes", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> APPLE_JUICE = ITEMS.register("apple_juice", () -> new Item(PVItemGroup.PROPERTIES.food(Foods.APPLE)));
	public static final RegistryObject<Item> CINNAMON = ITEMS.register("cinnamon", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> SEA_SALT = ITEMS.register("sea_salt", () -> new Item(PVItemGroup.PROPERTIES));
	//ALL DRINKABLE THINGS ARE BELOW
	public static final RegistryObject<Item> SHOT_SILVER_TEQUILA = ITEMS.register("shot_silver_tequila", () -> new DrinkItem(PVItemGroup.PROPERTIES, 16, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final RegistryObject<Item> SHOT_GOLDEN_TEQUILA = ITEMS.register("shot_golden_tequila", () -> new DrinkItem(PVItemGroup.PROPERTIES, 16, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final RegistryObject<Item> SHOT_RECTIFIED_SPIRIT = ITEMS.register("shot_rectified_spirit", () -> new DrinkItem(PVItemGroup.PROPERTIES, 16, 2.375, new ItemStack(Items.GLASS_BOTTLE)));
	public static final RegistryObject<Item> SHOT_VODKA = ITEMS.register("shot_vodka", () -> new DrinkItem(PVItemGroup.PROPERTIES, 16, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final RegistryObject<Item> SPICED_APPLE_MARGARITA = ITEMS.register("spiced_apple_margarita", () -> new DrinkItem(PVItemGroup.PROPERTIES, 32, 2, new ItemStack(PVItems.MARGARITA_GLASS.get()), new EffectInstance(Effects.LUCK, 18000), new EffectInstance(Effects.JUMP_BOOST, 12000), new EffectInstance(Effects.SPEED, 12000)));
	//BIG SHIM ENERGY
	public static final RegistryObject<Item> BARREL = ITEMS.register("barrel", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(PVItemGroup.MAIN)));
	
	public static final RegistryObject<Item> CINNAMON_SIGN = ITEMS.register("cinnamon_sign", () -> new PVSignItem(PVItemGroup.PROPERTIES.maxStackSize(16), PVBlocks.CINNAMON_SIGN.get(), PVBlocks.CINNAMON_WALL_SIGN.get()));
	
	//I LOVE THESE SO MUCH
	public static final RegistryObject<Item> CINNABON_GLAZE = ITEMS.register("cinnabon_glaze", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> CINNABON_FILLING = ITEMS.register("cinnabon_filling", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> CINNABON_DOUGH = ITEMS.register("cinnabon_dough", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FILLED_CINNABON_DOUGH = ITEMS.register("filled_cinnabon_dough", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> BAKED_CINNABON_DOUGH = ITEMS.register("baked_cinnabon_dough", () -> new Item(PVItemGroup.PROPERTIES));
	public static final RegistryObject<Item> CINNABON = ITEMS.register("cinnabon", () -> new Item(PVItemGroup.PROPERTIES.food(Foods.PUMPKIN_PIE)));
}