/*
    Project Vinum - PVBlocks.java
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
package io.vinum.block;

import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import io.vinum.block.trees.CinnamonTree;
import io.vinum.common.PVDefines;

public final class PVBlocks {
	
	public static final DeferredRegister<Block> BLOCKS = new DeferredRegister<>(ForgeRegistries.BLOCKS, PVDefines.MODID);
	
	private static final Set<RegistryObject<Block>> PURE_BLOCKS = new java.util.HashSet<>();
	
	//public static final RegistryObject<Block> BARREL = register("barrel", () -> new BarrelBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.5F, 2.5F)));
	
	public static final RegistryObject<Block> STEEL_BRAZIER = register("steel_brazier", () -> new SteelBrazierBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).lightValue(15).notSolid()));
	public static final RegistryObject<Block> STEEL_POT = register("steel_pot", () -> new SteelPotBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()));
	public static final RegistryObject<Block> STEEL_COIL = register("steel_coil", () -> new SteelCoilBlock(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()));
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_1 = register("still_multiblock_part_1", () -> new StillMultiblockPart1Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).lightValue(13).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_2 = register("still_multiblock_part_2", () -> new StillMultiblockPart2Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_3 = register("still_multiblock_part_3", () -> new StillMultiblockPart3Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	public static final RegistryObject<Block> STILL_MULTIBLOCK_PART_4 = register("still_multiblock_part_4", () -> new StillMultiblockPart4Block(Block.Properties.create(Material.IRON, MaterialColor.STONE).hardnessAndResistance(2.0F, 2.0F).notSolid()), false);
	
	public static final RegistryObject<Block> CINNAMON_LOG = register("cinnamon_log", () -> new RotatedPillarBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.0F).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> STRIPPED_CINNAMON_LOG = register("stripped_cinnamon_log", () -> new RotatedPillarBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.0F).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> CINNAMON_WOOD = register("cinnamon_wood", () -> new RotatedPillarBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.0F).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> STRIPPED_CINNAMON_WOOD = register("stripped_cinnamon_wood", () -> new RotatedPillarBlock(Block.Properties.create(Material.WOOD, MaterialColor.WOOD).hardnessAndResistance(2.0F).sound(SoundType.WOOD)));
	public static final RegistryObject<Block> CINNAMON_LEAVES = register("cinnamon_leaves", () -> new LeavesBlock(Block.Properties.create(Material.LEAVES).hardnessAndResistance(0.2F).tickRandomly().sound(SoundType.PLANT).notSolid()));
	public static final RegistryObject<Block> CINNAMON_PLANKS = register("cinnamon_planks", () -> new Block(Block.Properties.create(Material.WOOD, MaterialColor.ADOBE).hardnessAndResistance(2.0F, 3.0F).sound(SoundType.WOOD)));
	
	public static final RegistryObject<Block> CINNAMON_SIGN = register("cinnamon_sign", () -> new PVStandingSignBlock(Block.Properties.create(Material.WOOD).doesNotBlockMovement().hardnessAndResistance(1.0F).sound(SoundType.WOOD), PVWoodType.CINNAMON), false);
	public static final RegistryObject<Block> CINNAMON_WALL_SIGN = register("cinnamon_wall_sign", () -> new PVWallSignBlock(Block.Properties.create(Material.WOOD).doesNotBlockMovement().hardnessAndResistance(1.0F).sound(SoundType.WOOD).lootFrom(CINNAMON_SIGN.get()), PVWoodType.CINNAMON), false);
	public static final RegistryObject<Block> CINNAMON_DOOR = register("cinnamon_door", () -> new PVDoorBlock(Block.Properties.create(Material.WOOD, MaterialColor.ADOBE).hardnessAndResistance(3.0F).sound(SoundType.WOOD).notSolid()), false);
	public static final RegistryObject<Block> CINNAMON_STAIRS = register("cinnamon_stairs", () -> new PVStairsBlock(CINNAMON_PLANKS.get().getDefaultState(), Block.Properties.from(CINNAMON_PLANKS.get())));
	
	public static final RegistryObject<Block> CROP_AGAVE = register("crop_agave", () -> new AgaveCropBlock(Block.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly().hardnessAndResistance(0.0F, 0.F).sound(SoundType.CROP)), false);
	public static final RegistryObject<Block> CINNAMON_SAPLING = register("cinnamon_sapling", () -> new PVSaplingBlock(new CinnamonTree(), Block.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly().hardnessAndResistance(0.0F, 0.0F).sound(SoundType.PLANT)));

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
