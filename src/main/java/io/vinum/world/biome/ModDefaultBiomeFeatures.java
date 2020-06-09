package io.vinum.world.biome;

import com.google.common.collect.ImmutableList;

import io.vinum.block.ModBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliageplacer.BlobFoliagePlacer;
import net.minecraft.world.gen.treedecorator.BeehiveTreeDecorator;
import net.minecraftforge.common.IPlantable;

public class ModDefaultBiomeFeatures {
	
	public static final TreeFeatureConfig CINNAMON_TREE_WITH_BEEHIVE = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.getDefaultState()), new BlobFoliagePlacer(0, 0))).decorators(ImmutableList.of(new BeehiveTreeDecorator(0.05F))).setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
	public static final TreeFeatureConfig CINNAMON_TREE_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.getDefaultState()), new BlobFoliagePlacer(2, 0))).baseHeight(4).heightRandA(2).foliageHeight(3).ignoreVines().setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
	
}