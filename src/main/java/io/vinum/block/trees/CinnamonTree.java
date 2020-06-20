/*
    Project Vinum - CinnamonTree.java
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
package io.vinum.block.trees;

import java.util.Random;

import javax.annotation.Nullable;

import io.vinum.world.biome.ModDefaultBiomeFeatures;
import io.vinum.worldgen.ModFeatures;
import net.minecraft.block.trees.Tree;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

public class CinnamonTree extends Tree {
	
	@Nullable
	protected ConfiguredFeature<TreeFeatureConfig, ?> getTreeFeature(Random randomIn, boolean hasBeehive) {
		
		return randomIn.nextInt(10) == 0 ? ModFeatures.CINNAMON_TREE.get().withConfiguration(hasBeehive ? ModDefaultBiomeFeatures.CINNAMON_TREE_WITH_BEEHIVE_CONFIG : ModDefaultBiomeFeatures.CINNAMON_TREE_CONFIG) : ModFeatures.CINNAMON_TREE.get().withConfiguration(hasBeehive ? ModDefaultBiomeFeatures.CINNAMON_TREE_WITH_MORE_BEEHIVES_CONFIG : ModDefaultBiomeFeatures.CINNAMON_TREE_CONFIG);
		
	}
	
}