package io.vinum.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Set;
import java.util.function.Supplier;

import io.vinum.common.Defines;

public final class ModBlocks {
	
	public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, Defines.MODID);
	
	private static final Set<RegistryObject<Block>> PURE_BLOCKS = new java.util.HashSet<>();
	
	//public static final RegistryObject<Block> BARREL = register("barrel", () -> new BarrelBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.5F, 2.5F)));
	
	public static final RegistryObject<Block> STEEL_BRAZIER = register("steel_brazier", () -> new SteelBrazierBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).lightValue(15).notSolid()));
	public static final RegistryObject<Block> STEEL_POT = register("steel_pot", () -> new SteelPotBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()));
	public static final RegistryObject<Block> STEEL_COIL = register("steel_coil", () -> new SteelCoilBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()));
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_1 = register("still_multiblock_part_1", () -> new StillMultiblockPart1Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_2 = register("still_multiblock_part_2", () -> new StillMultiblockPart2Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_3 = register("still_multiblock_part_3", () -> new StillMultiblockPart3Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_4 = register("still_multiblock_part_4", () -> new StillMultiblockPart4Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	
	public static final RegistryObject<Block> CINNAMON_LOG = register("cinnamon_log", () -> new Block(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.0F, 2.0F).sound(SoundType.WOOD)));

	public static final RegistryObject<Block> CROP_AGAVE = register("crop_agave", () -> new AgaveCropBlock(Block.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly().hardnessAndResistance(0.0F, 0.F).sound(SoundType.CROP)), false);
	public static final RegistryObject<Block> SAPLING_CINNAMON = register("sapling_cinnamon", () -> new CinnamonTreeSapling(treeIn, Block.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly().hardnessAndResistance(0.0F, 0.0F).sound(SoundType.PLANT)));

	/**
	 * @return {@code true} if the given {@code Block} requires an associated {@code BlockItem}.
	 * 			Most blocks will want to have {@code BlockItems} as they want to be in inventory.
	 */
	public static boolean needsItemBlock(Block block) {

		for (RegistryObject<Block> pureBlock : PURE_BLOCKS) {
			if (pureBlock.get() == block) {
				return false;
			}
		} return true;
	}
	
	/**
	 * Adds the given supplier associated with the given name to the list of entries to be registered.
	 *
	 * @param name the new entry's name, it will automatically be prefixed.
	 * @param sup a factory for the new entry, it should return a new instance every time it is called.
	 * @param hasItem whether this {@code Block} is intended to have an associated {@code BlockItem}.
	 *
	 * @return {@code RegistryObject} that will be updated with when the entries in the registry change.
	 */
	private static RegistryObject<Block> register(String name, Supplier<? extends Block> sup, boolean hasItem) {
		
		RegistryObject<Block> registryObject = BLOCKS.register(name, sup);
		if (!hasItem) PURE_BLOCKS.add(registryObject);

		return registryObject;
	}

	/**
	 * Default registration method that registers blocks as {@code BlockItem} owners.
	 * @see #register(String, Supplier, boolean)
	 */
	private static RegistryObject<Block> register(String name, Supplier<? extends Block> sup) {
		return register(name, sup, true);
	}
}
