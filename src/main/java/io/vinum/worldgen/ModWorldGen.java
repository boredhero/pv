package io.vinum.worldgen;

import java.util.HashSet;

import com.google.common.collect.ImmutableList;
import io.vinum.block.AgaveCropBlock;
import io.vinum.block.ModBlocks;
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
	
	public static final BlockClusterFeatureConfig AGAVE_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CROP_AGAVE.get().getDefaultState().with(AgaveCropBlock.AGE, 2)), new SimpleBlockPlacer())).tries(5).whitelist(new HashSet<Block>((BlockTags.SAND.getAllElements()))).build();
	
	public static void addFeatures () {
		
		for (Biome biome : ForgeRegistries.BIOMES) {
			
			if (biome.getCategory() == Biome.Category.DESERT || biome.getCategory() == Biome.Category.MESA) {
				
				biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Feature.RANDOM_PATCH.withConfiguration(AGAVE_CONFIG).withPlacement(Placement.CHANCE_HEIGHTMAP_DOUBLE.configure(new ChanceConfig(15))));
				
			}
			
			if (biome.getCategory() == Biome.Category.JUNGLE) {
				
				biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Feature.RANDOM_SELECTOR.withConfiguration(new MultipleRandomFeatureConfig(ImmutableList.of(ModFeatures.CINNAMON_TREE.get().withConfiguration(ModDefaultBiomeFeatures.CINNAMON_TREE_CONFIG).withChance(0.1F)), ModFeatures.CINNAMON_TREE.get().withConfiguration(ModDefaultBiomeFeatures.CINNAMON_TREE_WITH_BEEHIVE_CONFIG))).withPlacement(Placement.COUNT_EXTRA_HEIGHTMAP.configure(new AtSurfaceWithExtraConfig(50, 0.1F, 1))));
				
			}
			
		}
		
	}
	
}