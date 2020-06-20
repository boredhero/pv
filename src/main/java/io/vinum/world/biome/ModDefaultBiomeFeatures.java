/*
    Project Vinum - ModDefaultBiomeFeatures.java
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
package io.vinum.world.biome;

import com.google.common.collect.ImmutableList;

import io.vinum.block.ModBlocks;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliageplacer.BlobFoliagePlacer;
import net.minecraft.world.gen.treedecorator.BeehiveTreeDecorator;
import net.minecraftforge.common.IPlantable;

public class ModDefaultBiomeFeatures {
	
	public static final TreeFeatureConfig CINNAMON_TREE_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(ModBlocks.CINNAMON_LEAVES.get().getDefaultState()), new BlobFoliagePlacer(2, 1))).baseHeight(4).heightRandA(4).foliageHeight(2).ignoreVines().setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
	public static final TreeFeatureConfig CINNAMON_TREE_WITH_BEEHIVE_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(ModBlocks.CINNAMON_LEAVES.get().getDefaultState()), new BlobFoliagePlacer(0, 0))).decorators(ImmutableList.of(new BeehiveTreeDecorator(0.05F))).setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
	public static final TreeFeatureConfig CINNAMON_TREE_WITH_MORE_BEEHIVES_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(ModBlocks.CINNAMON_LEAVES.get().getDefaultState()), new BlobFoliagePlacer(2, 1))).baseHeight(4).heightRandA(4).foliageHeight(2).ignoreVines().decorators(ImmutableList.of(new BeehiveTreeDecorator(0.05F))).setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
	
}