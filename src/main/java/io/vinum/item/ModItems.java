package io.vinum.item;

import io.vinum.common.Defines;
import net.minecraft.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;
import java.util.function.Supplier;

public final class ModItems {

	public static final DeferredRegister<Item> ITEMS = new DeferredRegister<>(ForgeRegistries.ITEMS, Defines.MODID);

	//Add base items!
	public static final RegistryObject<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> STEEL_NUGGER = ITEMS.register("steel_nugger", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> UNFERMENTED_AGAVE_WORT = ITEMS.register("unfermented_agave_wort", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> FERMENTED_AGAVE_WORT = ITEMS.register("fermented_agave_wort", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> AGAVE_SEEDS = ITEMS.register("agave_seeds", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> AGAVE = ITEMS.register("agave", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> COOKED_AGAVE_PULP = ITEMS.register("cooked_agave_pulp", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> FIFTH_BOTTLE_EMPTY = ITEMS.register("fifth_bottle_empty", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> FIFTH_SILVER_TEQUILA = ITEMS.register("fifth_silver_tequila", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> FIFTH_GOLDEN_TEQUILA = ITEMS.register("fifth_golden_tequila", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHOT_GLASS_EMPTY = ITEMS.register("shot_glass_empty", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHOT_SILVER_TEQUILA = ITEMS.register("shot_silver_tequila", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SHOT_GOLDEN_TEQUILA = ITEMS.register("shot_golden_tequila", () -> new Item(new Item.Properties()));

}
