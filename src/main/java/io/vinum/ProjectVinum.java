package io.vinum;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.common.MinecraftForge;
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
	
}