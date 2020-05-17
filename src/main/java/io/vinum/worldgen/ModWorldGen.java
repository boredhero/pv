package io.vinum.worldgen;

import io.vinum.block.AgaveCropBlock;
import io.vinum.block.ModBlocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.blockplacer.SimpleBlockPlacer;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.BlockClusterFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.placement.FrequencyConfig;
import net.minecraft.world.gen.placement.Placement;
import net.minecraftforge.registries.ForgeRegistries;

public class ModWorldGen {
	
	public static final BlockClusterFeatureConfig AGAVE_CONFIG = (new BlockClusterFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CROP_AGAVE.get().getDefaultState().with(AgaveCropBlock.AGE, 2)), new SimpleBlockPlacer())).tries(5).build();
	
	public static void addFeatures () {
		
		for (Biome biome : ForgeRegistries.BIOMES) {
			
			if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS || biome == Biomes.DESERT_LAKES || biome == Biomes.BADLANDS || biome == Biomes.BADLANDS_PLATEAU) {
				
				biome.addFeature(GenerationStage.Decoration.VEGETAL_DECORATION, Feature.RANDOM_PATCH.withConfiguration(AGAVE_CONFIG).withPlacement(Placement.COUNT_HEIGHTMAP_DOUBLE.configure(new FrequencyConfig(1))));
				
			}
			
		}
		
	}
	
}