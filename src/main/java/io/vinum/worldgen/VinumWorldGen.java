package io.vinum.worldgen;

import io.vinum.common.Defines;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class VinumWorldGen {
    public static Feature<NoFeatureConfig> agave_wg;

    public static void registerAll(RegistryEvent.Register<Feature<?>> event){
        if (!event.getName().equals(ForgeRegistries.FEATURES.getRegistryName())){
            return;
        }
        IForgeRegistry<Feature<?>> reg = event.getRegistry();
        agave_wg = register(reg, new FeatureAgave(NoFeatureConfig::deserialize), "agave_wg");
    }

    private static <V extends R, R extends IForgeRegistryEntry<R>> V register(IForgeRegistry<R> registry, V value,
			String name) {
		ResourceLocation id = new ResourceLocation(Defines.MODID, name);;
		value.setRegistryName(id);
		registry.register(value);
		return value;
	}
}