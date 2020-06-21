/*
    Project Vinum - PVFeatures.java
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

import java.util.function.Supplier;

import com.mojang.datafixers.Dynamic;

import io.vinum.block.PVBlocks;
import io.vinum.common.PVDefines;
import io.vinum.worldgen.features.CinnamonTreeFeature;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliageplacer.BlobFoliagePlacer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class PVFeatures {
	
	public static final DeferredRegister<Feature<?>> FEATURES = new DeferredRegister<>(ForgeRegistries.FEATURES, PVDefines.MODID);
	
	public static final RegistryObject<Feature<TreeFeatureConfig>> CINNAMON_TREE = registerTree("cinnamon_tree", () -> new CinnamonTreeFeature(PVFeatures::cinnamonTreeConfig));
	
	public static RegistryObject<Feature<TreeFeatureConfig>> registerTree(String name, Supplier<? extends Feature<?>> sup) {
		
		RegistryObject<Feature<TreeFeatureConfig>> registryObject = (RegistryObject<Feature<TreeFeatureConfig>>) FEATURES.register(name, sup);
		
		return registryObject;
		
	}
	
	public static <T> TreeFeatureConfig cinnamonTreeConfig(Dynamic<T> dynamic) {
		
		return (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(PVBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.getDefaultState()), new BlobFoliagePlacer(2, 0))).baseHeight(4).heightRandA(2).foliageHeight(3).ignoreVines().setSapling((IPlantable)PVBlocks.CINNAMON_SAPLING.get()).build();
		
	}
	
}