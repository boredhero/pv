package io.vinum.item;

import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import io.vinum.item.drinks.DrinkItem;
import io.vinum.block.ModBlocks;
import io.vinum.common.Defines;

public final class ModItems {

	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Defines.MODID);

	//Metals
	public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(new Item.Properties()));
	//Agave Products
	public static final DeferredItem<Item> UNFERMENTED_AGAVE_WORT = ITEMS.register("unfermented_agave_wort", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> FERMENTED_AGAVE_WORT = ITEMS.register("fermented_agave_wort", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> AGAVE_SEEDS = ITEMS.register("agave_seeds", () -> new ItemNameBlockItem(ModBlocks.CROP_AGAVE.get(), (new Item.Properties())));
	public static final DeferredItem<Item> AGAVE = ITEMS.register("agave", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> COOKED_AGAVE_PULP = ITEMS.register("cooked_agave_pulp", () -> new Item(new Item.Properties().food(Foods.DRIED_KELP)));
	//Glassware and glass crafting
	public static final DeferredItem<Item> MOLTEN_GLASS = ITEMS.register("molten_glass", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> FIFTH_BOTTLE_EMPTY = ITEMS.register("fifth_bottle_empty", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> PACK_OF_SHOT_GLASSES = ITEMS.register("pack_of_shot_glasses", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> MARGARITA_GLASS = ITEMS.register("margarita_glass", () -> new Item(new Item.Properties()));
	//Fifths of Booze
	public static final DeferredItem<Item> FIFTH_SILVER_TEQUILA = ITEMS.register("fifth_silver_tequila", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> FIFTH_GOLDEN_TEQUILA = ITEMS.register("fifth_golden_tequila", () -> new AgedBeverageItem(new Item.Properties()));
	//public static final DeferredItem<Item> FIFTH_RECTIFIED_SPIRIT = ITEMS.register("fifth_rectified_spirits", () -> new Item(new Item.Properties()));
	//Items used in crafting that must not break when used.
	public static final DeferredItem<Item> JUICER = ITEMS.register("juicer", () -> new DontBreakOnCraftItem(new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> ICE_PICK = ITEMS.register("ice_pick", () -> new DontBreakOnCraftItem(new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> COCKTAIL_SHAKER = ITEMS.register("cocktail_shaker", () -> new DontBreakOnCraftItem(new Item.Properties().stacksTo(1)));
	public static final DeferredItem<Item> MARGARITA_GLASS_MOLD = ITEMS.register("margarita_glass_mold", () -> new DontBreakOnCraftItem(new Item.Properties().stacksTo(1)));
	//Misc Cocktail Ingredients
	public static final DeferredItem<Item> ICE_CUBES = ITEMS.register("ice_cubes", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> APPLE_JUICE = ITEMS.register("apple_juice", () -> new Item(new Item.Properties().food(Foods.APPLE)));
	public static final DeferredItem<Item> CINNAMON = ITEMS.register("cinnamon", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> SEA_SALT = ITEMS.register("sea_salt", () -> new Item(new Item.Properties()));
	//ALL DRINKABLE THINGS ARE BELOW
	public static final DeferredItem<Item> SHOT_SILVER_TEQUILA = ITEMS.register("shot_silver_tequila", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final DeferredItem<Item> SHOT_GOLDEN_TEQUILA = ITEMS.register("shot_golden_tequila", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final DeferredItem<Item> SPICED_APPLE_MARGARITA = ITEMS.register("spiced_apple_margarita", () -> new DrinkItem(new Item.Properties(), 32, 2, new ItemStack(ModItems.MARGARITA_GLASS.get()), new MobEffectInstance(MobEffects.LUCK, 18000), new MobEffectInstance(MobEffects.JUMP, 12000), new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12000)));
	//BIG SHIM ENERGY
	public static final DeferredItem<BarrelItem> BARREL = ITEMS.register("barrel", () -> new BarrelItem(ModBlocks.BARREL.get(), new Item.Properties()));


	// Wine content unified from the original project-vinum prototype.
	public static final DeferredItem<Item> CORK_BARK = ITEMS.register("cork_bark", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_CORK = ITEMS.register("wine_cork", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_PRESS = ITEMS.register("wine_press", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_YEAST = ITEMS.register("wine_yeast", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register("lead_ingot", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> LEAD_POWDER = ITEMS.register("lead_powder", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> IRON_POWDER = ITEMS.register("iron_powder", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_EMPTY = ITEMS.register("wine_bottle_empty", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_CHARDONNAY = ITEMS.register("wine_bottle_chardonnay", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_SAUVIGNON_BLANC = ITEMS.register("wine_bottle_sauvignon_blanc", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_CABERNET_SAUVIGNON = ITEMS.register("wine_bottle_cabernet_sauvignon", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_MERLOT = ITEMS.register("wine_bottle_merlot", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_PINOT_NOIR = ITEMS.register("wine_bottle_pinot_noir", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_BOTTLE_ZINFANDEL = ITEMS.register("wine_bottle_zinfandel", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> WINE_GLASS_EMPTY = ITEMS.register("wine_glass_empty", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> WINE_GLASS_CHARDONNAY = ITEMS.register("wine_glass_chardonnay", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> WINE_GLASS_SAUVIGNON_BLANC = ITEMS.register("wine_glass_sauvignon_blanc", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> WINE_GLASS_CABERNET_SAUVIGNON = ITEMS.register("wine_glass_cabernet_sauvignon", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> WINE_GLASS_MERLOT = ITEMS.register("wine_glass_merlot", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> WINE_GLASS_PINOT_NOIR = ITEMS.register("wine_glass_pinot_noir", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> WINE_GLASS_ZINFANDEL = ITEMS.register("wine_glass_zinfandel", () -> new DrinkItem(new Item.Properties(), 32, 1, new ItemStack(WINE_GLASS_EMPTY.get())));
	public static final DeferredItem<Item> GRAPES_CHARDONNAY = ITEMS.register("grapes_chardonnay", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPES_SAUVIGNON_BLANC = ITEMS.register("grapes_sauvignon_blanc", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPES_CABERNET_SAUVIGNON = ITEMS.register("grapes_cabernet_sauvignon", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPES_MERLOT = ITEMS.register("grapes_merlot", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPES_PINOT_NOIR = ITEMS.register("grapes_pinot_noir", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPES_ZINFANDEL = ITEMS.register("grapes_zinfandel", () -> new Item(new Item.Properties().food(Foods.SWEET_BERRIES)));
	public static final DeferredItem<Item> GRAPE_JUICE_CHARDONNAY = ITEMS.register("grape_juice_chardonnay", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_JUICE_SAUVIGNON_BLANC = ITEMS.register("grape_juice_sauvignon_blanc", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_JUICE_CABERNET_SAUVIGNON = ITEMS.register("grape_juice_cabernet_sauvignon", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_JUICE_MERLOT = ITEMS.register("grape_juice_merlot", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_JUICE_PINOT_NOIR = ITEMS.register("grape_juice_pinot_noir", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_JUICE_ZINFANDEL = ITEMS.register("grape_juice_zinfandel", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_CHARDONNAY = ITEMS.register("grape_seeds_chardonnay", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_SAUVIGNON_BLANC = ITEMS.register("grape_seeds_sauvignon_blanc", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_CABERNET_SAUVIGNON = ITEMS.register("grape_seeds_cabernet_sauvignon", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_MERLOT = ITEMS.register("grape_seeds_merlot", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_PINOT_NOIR = ITEMS.register("grape_seeds_pinot_noir", () -> new Item(new Item.Properties()));
	public static final DeferredItem<Item> GRAPE_SEEDS_ZINFANDEL = ITEMS.register("grape_seeds_zinfandel", () -> new Item(new Item.Properties()));

	// Only placed blocks with inventory forms receive block items.
	public static final DeferredItem<BlockItem> STEEL_BRAZIER = ITEMS.register("steel_brazier", () -> new BlockItem(ModBlocks.STEEL_BRAZIER.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> STEEL_POT = ITEMS.register("steel_pot", () -> new BlockItem(ModBlocks.STEEL_POT.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> STEEL_COIL = ITEMS.register("steel_coil", () -> new BlockItem(ModBlocks.STEEL_COIL.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_LEAD_ORE = ITEMS.register("block_lead_ore", () -> new BlockItem(ModBlocks.BLOCK_LEAD_ORE.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_LEAD = ITEMS.register("block_lead", () -> new BlockItem(ModBlocks.BLOCK_LEAD.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_EMPTY = ITEMS.register("block_wine_barrel_empty", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_EMPTY.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_CHARDONNAY = ITEMS.register("block_wine_barrel_chardonnay", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_CHARDONNAY.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_SAUVIGNON_BLANC = ITEMS.register("block_wine_barrel_sauvignon_blanc", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_SAUVIGNON_BLANC.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_CABERNET_SAUVIGNON = ITEMS.register("block_wine_barrel_cabernet_sauvignon", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_CABERNET_SAUVIGNON.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_MERLOT = ITEMS.register("block_wine_barrel_merlot", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_MERLOT.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_PINOT_NOIR = ITEMS.register("block_wine_barrel_pinot_noir", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_PINOT_NOIR.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_WINE_BARREL_ZINFANDEL = ITEMS.register("block_wine_barrel_zinfandel", () -> new BarrelItem(ModBlocks.BLOCK_WINE_BARREL_ZINFANDEL.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_LEADED_SAND = ITEMS.register("block_leaded_sand", () -> new BlockItem(ModBlocks.BLOCK_LEADED_SAND.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_FERROUS_SAND = ITEMS.register("block_ferrous_sand", () -> new BlockItem(ModBlocks.BLOCK_FERROUS_SAND.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_REFINED_SAND = ITEMS.register("block_refined_sand", () -> new BlockItem(ModBlocks.BLOCK_REFINED_SAND.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_DARK_WINE_GLASS = ITEMS.register("block_dark_wine_glass", () -> new BlockItem(ModBlocks.BLOCK_DARK_WINE_GLASS.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> BLOCK_FINE_CRYSTAL_GLASS = ITEMS.register("block_fine_crystal_glass", () -> new BlockItem(ModBlocks.BLOCK_FINE_CRYSTAL_GLASS.get(), new Item.Properties()));

	// Oak-matured tequila stages share the same fluid and retain their batch age.
	public static final DeferredItem<Item> FIFTH_REPOSADO_TEQUILA = ITEMS.register("fifth_reposado_tequila", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> FIFTH_ANEJO_TEQUILA = ITEMS.register("fifth_anejo_tequila", () -> new AgedBeverageItem(new Item.Properties()));
	public static final DeferredItem<Item> FIFTH_EXTRA_ANEJO_TEQUILA = ITEMS.register("fifth_extra_anejo_tequila", () -> new AgedBeverageItem(new Item.Properties()));
}
