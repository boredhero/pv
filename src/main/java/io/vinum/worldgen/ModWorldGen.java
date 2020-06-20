/*
    Project Vinum - ModWorldGen.java
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
package io.vinum.worldgen;

import java.util.HashSet;

import com.google.common.collect.ImmutableList;
import io.vinum.block.AgaveCropBlock;
import io.vinum.block.ModBlocks;
import io.vinum.config.PVConfig;
import io.vinum.world.biome.ModDefaultBiomeFeatures;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.blockplacer.SimpleBlockPlacer;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.BlockClusterFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.MultipleRandomFeatureConfig;
import net.minecraft.world.gen.placement.AtSurfaceWithExtraConfig;
import net.minecraft.world.gen.placement.ChanceConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.registries.ForgeRegistries;

public class ModWorldGen {
	
	//Config
	final static int AGAVE_SPAWN_CHANCES = PVConfig.agave_spawn_chances;
	//TODO: Figure out how to do a similar thing for the tree...

	public static final BlockClusterFeatureConfig AGAVE_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CROP_AGAVE.get().getDefaultState().with(AgaveCropBlock.AGE, 2)), new SimpleBlockPlacer())).tries(5).whitelist(new HashSet<Block>((BlockTags.SAND.getAllElements()))).build();
	
	public static void addFeatures () {
		
		for (Biome biome : ForgeRegistries.BIOMES) {
			
			if (biome.getCategory() == Biome.Category.DESERT || biome.getCategory() == Biome.Category.MESA) {
				
				biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Feature.RANDOM_PATCH.withConfiguration(AGAVE_CONFIG).withPlacement(Placement.CHANCE_HEIGHTMAP_DOUBLE.configure(new ChanceConfig(AGAVE_SPAWN_CHANCES))));
				
			}
			
			if (biome.getCategory() == Biome.Category.JUNGLE) {
				
				biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Feature.RANDOM_SELECTOR.withConfiguration(new MultipleRandomFeatureConfig(ImmutableList.of(ModFeatures.CINNAMON_TREE.get().withConfiguration(ModDefaultBiomeFeatures.CINNAMON_TREE_CONFIG).withChance(0.1F)), ModFeatures.CINNAMON_TREE.get().withConfiguration(ModDefaultBiomeFeatures.CINNAMON_TREE_WITH_BEEHIVE_CONFIG))).withPlacement(Placement.COUNT_EXTRA_HEIGHTMAP.configure(new AtSurfaceWithExtraConfig(50, 0.1F, 1))));
				
			}
			
		}
		
	}
	
}