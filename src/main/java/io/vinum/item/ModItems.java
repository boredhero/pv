package io.vinum.item;

import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;

import io.vinum.common.Defines;
import io.vinum.item.drinks.DrinkItem;

public final class ModItems {

	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, Defines.MODID);

	//Add base items!
	public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(ModItemGroup.PROPERTIES));
	
	public static final RegistryObject<Item> JUICER = ITEMS.register("juicer", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(ModItemGroup.MAIN)));
	public static final RegistryObject<Item> ICE_PICK = ITEMS.register("ice_pick", () -> new DontBreakOnCraftItem(new Item.Properties().maxStackSize(1).group(ModItemGroup.MAIN)));
	
	public static final RegistryObject<Item> UNFERMENTED_AGAVE_WORT = ITEMS.register("unfermented_agave_wort", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> FERMENTED_AGAVE_WORT = ITEMS.register("fermented_agave_wort", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> AGAVE_SEEDS = ITEMS.register("agave_seeds", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> AGAVE = ITEMS.register("agave", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> COOKED_AGAVE_PULP = ITEMS.register("cooked_agave_pulp", () -> new Item(ModItemGroup.PROPERTIES.food(Foods.DRIED_KELP)));
	public static final RegistryObject<Item> FIFTH_BOTTLE_EMPTY = ITEMS.register("fifth_bottle_empty", () -> new Item(ModItemGroup.PROPERTIES));
	
	/** TODO vvv Use "DrinkItem" for all drinkable items. **/
	
	public static final RegistryObject<Item> FIFTH_SILVER_TEQUILA = ITEMS.register("fifth_silver_tequila", () -> new DrinkItem(ModItemGroup.PROPERTIES, 32, 1, new ItemStack(Items.GLASS_BOTTLE)));
	
	public static final RegistryObject<Item> FIFTH_GOLDEN_TEQUILA = ITEMS.register("fifth_golden_tequila", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> PACK_OF_SHOT_GLASSES = ITEMS.register("pack_of_shot_glasses", () -> new Item(ModItemGroup.PROPERTIES));
	//Stuff for cocktails....Experimental....don't add textures just yet.
	public static final RegistryObject<Item> FIFTH_RECTIFIED_SPIRIT = ITEMS.register("fifth_rectified_spirits", () -> new Item(ModItemGroup.PROPERTIES));
	//public static final RegistryObject<Item> JUICER = ITEMS.register("juicer", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> ICE_CUBES = ITEMS.register("ice_cubes", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> SIMPLE_SYRUP = ITEMS.register("simple_syrup", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> MOLTEN_GLASS = ITEMS.register("molten_glass", () -> new Item(ModItemGroup.PROPERTIES));
	//public static final RegistryObject<Item> ROCKS_GLASS_MOLD = ITEMS.register("tumbler_glass_mold", () -> new Item(ModItemGroup.PROPERTIES));
	//public static final RegistryObject<Item> COCKTAIL_GLASS_MOLD = ITEMS.register("cocktail_glass_mold", () -> new Item(ModItemGroup.PROPERTIES));
	//public static final RegistryObject<Item> HIGHBALL_GLASS_MOLD = ITEMS.register("highball_glass_mold", () -> new Item(ModItemGroup.PROPERTIES));
	//COCKTAIL: APPLE MARGARITA
	public static final RegistryObject<Item> MARGARITA_GLASS_MOLD = ITEMS.register("margarita_glass_mold", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> APPLE_JUICE = ITEMS.register("apple_juice", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> CINNAMON = ITEMS.register("cinnamon", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> MARGARITA_GLASS = ITEMS.register("margarita_glass", () -> new Item(ModItemGroup.PROPERTIES));
	public static final RegistryObject<Item> SPICED_APPLE_MARGARITA = ITEMS.register("spiced_apple_margarita", () -> new Item(ModItemGroup.PROPERTIES));

	//ALL DRINKABLE THINGS ARE BELOW
	public static final RegistryObject<Item> SHOT_SILVER_TEQUILA = ITEMS.register("shot_silver_tequila", () -> new DrinkItem(ModItemGroup.PROPERTIES, 32, 1, new ItemStack(Items.GLASS_BOTTLE)));
	public static final RegistryObject<Item> SHOT_GOLDEN_TEQUILA = ITEMS.register("shot_golden_tequila", () -> new DrinkItem(ModItemGroup.PROPERTIES, 32, 1, new ItemStack(Items.GLASS_BOTTLE)));
	
}