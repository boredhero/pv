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