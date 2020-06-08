package io.vinum.block;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.trees.Tree;
import net.minecraft.world.biome.DefaultBiomeFeatures;
import net.minecraft.world.gen.blockstateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliageplacer.BlobFoliagePlacer;

public class CinnamonTree extends Tree {

    //For more examples of these, look in DefaultBiomeFeature.class. Mojang used a seperate class to declare them before
    //using them in their Tree classes, but I don't think that's necessary for us.

    public static final BlockState CINNAMON_LOG = ModBlocks.CINNAMON_LOG.get().getDefaultState();
    public static final BlockState JUNGLE_LEAVES = Blocks.JUNGLE_LEAVES.getDefaultState();

    //public static final TreeFeatureConfig OAK_TREE_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(OAK_LOG), new SimpleBlockStateProvider(OAK_LEAVES), new BlobFoliagePlacer(2, 0))).baseHeight(4).heightRandA(2).foliageHeight(3).ignoreVines().setSapling((net.minecraftforge.common.IPlantable)Blocks.OAK_SAPLING).build();
    public static final TreeFeatureConfig CINNAMON_TREE_CONFIG = (new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(CINNAMON_LOG), new SimpleBlockStateProvider(JUNGLE_LEAVES), new BlobFoliagePlacer(2, 0))).baseHeight(4).heightRandA(2).foliageHeight(3).ignoreVines().setSapling((net.minecraftforge.common.IPlantable)ModBlocks.SAPLING_CINNAMON.get()).build(); 
    
    @Nullable
    protected ConfiguredFeature<TreeFeatureConfig, ?> getTreeFeature(Random randomIn, boolean bool){
       //This needs troubleshooting....
        // return randomIn.nextInt(10) == 0 ? Feature.FANCY_TREE.withConfiguration(bool ? DefaultBiomeFeatures.FANCY_TREE_WITH_MORE_BEEHIVES_CONFIG : DefaultBiomeFeatures.);
    }