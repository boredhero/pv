package io.vinum;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vinum.block.ModBlocks;
import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.capability.BACStorage;
import io.vinum.capability.IBAC;
import io.vinum.common.Defines;
import io.vinum.item.ModItems;

@Mod(Defines.MODID)
public class ProjectVinum {
	
	private static final Logger LOGGER = LogManager.getLogger();

	public ProjectVinum() {
		
		ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
		ModItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
		
		PVLogger.init(LogManager.getLogger());
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		
		MinecraftForge.EVENT_BUS.register(this);
		
	}
	
	private void setup(final FMLCommonSetupEvent event) {
		
		CapabilityManager.INSTANCE.register(IBAC.class, new BACStorage(), BAC::new);
		
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		
		RenderTypeLookup.setRenderLayer(ModBlocks.STEEL_BRAZIER.get(), RenderType.getCutoutMipped());
		
	}
	
	private void enqueueIMC(final InterModEnqueueEvent event) {
		
	}
	
	private void processIMC(final InterModProcessEvent event) {
		
	}
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		
	}
	
	@SubscribeEvent
	public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		
		if (event.getObject() instanceof PlayerEntity) {
			
			event.addCapability(new ResourceLocation(Defines.MODID, "bac_capability"), new BACCapability());
			
		}
		
	}
	
}