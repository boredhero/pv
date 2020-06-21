/*
    Project Vinum - CinnamonTreeFeature.java
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
package io.vinum.worldgen.features;

import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import com.mojang.datafixers.Dynamic;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.gen.IWorldGenerationReader;
import net.minecraft.world.gen.feature.AbstractSmallTreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

public class CinnamonTreeFeature extends AbstractSmallTreeFeature<TreeFeatureConfig> {
	
	public CinnamonTreeFeature(Function<Dynamic<?>, ? extends TreeFeatureConfig> config) {
		super(config);
		
	}
	
	public boolean func_225557_a_(IWorldGenerationReader generationReader, Random rand, BlockPos p_225557_3_, Set<BlockPos> p_225557_4_, Set<BlockPos> p_225557_5_, MutableBoundingBox p_225557_6_, TreeFeatureConfig p_225557_7_) {
		
		int i = p_225557_7_.baseHeight + rand.nextInt(p_225557_7_.heightRandA + 1) + rand.nextInt(p_225557_7_.heightRandB + 1);
		int j = p_225557_7_.trunkHeight >= 0 ? p_225557_7_.trunkHeight + rand.nextInt(p_225557_7_.trunkHeightRandom + 1) : i - (p_225557_7_.foliageHeight + rand.nextInt(p_225557_7_.foliageHeightRandom + 1));
		int k = p_225557_7_.foliagePlacer.func_225573_a_(rand, j, i, p_225557_7_);
		Optional<BlockPos> optional = this.func_227212_a_(generationReader, i, j, k, p_225557_3_, p_225557_7_);
		
		if (!optional.isPresent()) {
			
			return false;
			
		} else {
			
			BlockPos blockpos = optional.get();
			this.setDirtAt(generationReader, blockpos.down(), blockpos);
			p_225557_7_.foliagePlacer.func_225571_a_(generationReader, rand, p_225557_7_, i, j, k, blockpos, p_225557_5_);
			this.func_227213_a_(generationReader, rand, i, blockpos, p_225557_7_.trunkTopOffset + rand.nextInt(p_225557_7_.trunkTopOffsetRandom + 1), p_225557_4_, p_225557_6_, p_225557_7_);
			return true;
			
		}
		
	}
	
}