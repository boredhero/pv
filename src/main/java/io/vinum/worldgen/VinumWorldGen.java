package io.vinum.worldgen;

import io.vinum.common.Defines;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class VinumWorldGen {
	
	public static final DeferredRegister<Feature<?>> FEATURES = new DeferredRegister<>(ForgeRegistries.FEATURES, Defines.MODID);
	public static final RegistryObject<Feature<?>> AGAVE_FEATURE = FEATURES.register("agave_feature", () -> new FeatureAgave(NoFeatureConfig::deserialize));
	
	
}