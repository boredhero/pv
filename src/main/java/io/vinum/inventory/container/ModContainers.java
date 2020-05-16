package io.vinum.inventory.container;

import io.vinum.common.Defines;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModContainers {
	
	public static final DeferredRegister<ContainerType<?>> CONTAINERS = new DeferredRegister<>(ForgeRegistries.CONTAINERS, Defines.MODID);
	
	public static final RegistryObject<ContainerType<StillMasterContainer>> STILL_MASTER = CONTAINERS.register("still_master", () -> IForgeContainerType.create(StillMasterContainer::new));
	
}