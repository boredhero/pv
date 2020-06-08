package io.vinum.worldgen;

import io.vinum.common.Defines;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class VinumFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES = new DeferredRegister<>(ForgeRegistries.FEATURES, Defines.MODID);
    
    //Trees
    public static final RegistryObject<Feature<TreeFeatureConfig>> CINNAMON_TREE = FEATURES.register("cinnamon_tree", () -> new );
    
}