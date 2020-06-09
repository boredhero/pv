package io.vinum.worldgen;

import java.util.function.Supplier;

import com.mojang.datafixers.Dynamic;

import io.vinum.block.ModBlocks;
import io.vinum.common.Defines;
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

public class ModFeatures {
	
	public static final DeferredRegister<Feature<?>> FEATURES = new DeferredRegister<>(ForgeRegistries.FEATURES, Defines.MODID);
	
	public static final RegistryObject<Feature<TreeFeatureConfig>> CINNAMON_TREE = registerTree("cinnamon_tree", () -> new CinnamonTreeFeature(ModFeatures::cinnamonTreeConfig));
	
	public static RegistryObject<Feature<TreeFeatureConfig>> registerTree(String name, Supplier<? extends Feature<?>> sup) {
		
		RegistryObject<Feature<TreeFeatureConfig>> registryObject = (RegistryObject<Feature<TreeFeatureConfig>>) FEATURES.register(name, sup);
		
		return registryObject;
		
	}
	
	public static <T> TreeFeatureConfig cinnamonTreeConfig(Dynamic<T> dynamic) {
		
		return (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(ModBlocks.CINNAMON_LOG.get().getDefaultState()), new SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.getDefaultState()), new BlobFoliagePlacer(2, 0))).baseHeight(4).heightRandA(2).foliageHeight(3).ignoreVines().setSapling((IPlantable)ModBlocks.CINNAMON_SAPLING.get()).build();
		
	}
	
}