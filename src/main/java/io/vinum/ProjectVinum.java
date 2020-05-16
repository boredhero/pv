package io.vinum;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vinum.block.ModBlocks;
import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.capability.BACStorage;
import io.vinum.capability.IBAC;
import io.vinum.common.Defines;
import io.vinum.gui.GuiHandler;
import io.vinum.item.ModItems;
import io.vinum.item.drinks.IDrink;
import io.vinum.network.BACSyncMessage;
import io.vinum.network.NetworkLoader;

@Mod(Defines.MODID)
public class ProjectVinum {
	
	@SuppressWarnings("unused")
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
		MinecraftForge.EVENT_BUS.register(new GuiHandler());
		
	}
	
	private void setup(final FMLCommonSetupEvent event) {
		
		NetworkLoader.register();
		CapabilityManager.INSTANCE.register(IBAC.class, new BACStorage(), BAC::new);
		GuiHandler.initIcons();
		
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
	
	@SubscribeEvent
	public void updatePlayerBAC(LivingUpdateEvent event) {
		
		if (!event.getEntity().getEntityWorld().isRemote()) {
			
			if (event.getEntity() instanceof PlayerEntity) {
				
				ServerPlayerEntity target = (ServerPlayerEntity) event.getEntity();
				
				event.getEntity().getCapability(BACCapability.BAC_CAPABILITY, null).ifPresent(state -> {
					
					CompoundNBT nbt = new CompoundNBT();
                    Capability<IBAC> cap = BACCapability.BAC_CAPABILITY;
                    Capability.IStorage<IBAC> storage = cap.getStorage();
                    nbt.put(cap.getName(), storage.writeNBT(cap, state, null));
                    NetworkLoader.INSTANCE.send(PacketDistributor.PLAYER.with(() -> target), new BACSyncMessage(nbt));
                    
				});
				
				event.getEntity().getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
					
					@Override
					public void accept(@Nonnull IBAC iBAC) {
						
						if (iBAC.getBACLevel() > 0) {
							
							iBAC.addBACTicks(1);
							
						} else {
							
							iBAC.setBACTicks(0);
							
						}
						
						if (iBAC.getBACTicks() >= 4000 && iBAC.getBACLevel() > 0) {
							
							iBAC.removeBACLevel(1);
							iBAC.setBACTicks(0);
							
						}
						
						IDrink.updatePlayerBAC((PlayerEntity) event.getEntity(), iBAC.getBACLevel());
						
					}
					
				});
				
			}
			
		}
		
	}
	
}