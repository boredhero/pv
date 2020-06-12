package io.vinum;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameterSets;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vinum.block.ModBlocks;
import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.capability.BACStorage;
import io.vinum.capability.IBAC;
import io.vinum.client.renderer.color.ModColors;
import io.vinum.common.Defines;
import io.vinum.core.RegistryHandler;
import io.vinum.gui.GuiHandler;
import io.vinum.gui.screen.inventory.StillMasterScreen;
import io.vinum.inventory.container.ModContainers;
import io.vinum.item.drinks.IDrink;
import io.vinum.network.BACSyncMessage;
import io.vinum.network.NetworkLoader;
import io.vinum.worldgen.ModWorldGen;

@Mod(Defines.MODID)
public class ProjectVinum {
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();
	
	public ProjectVinum() {
		
		RegistryHandler.registerDeferred(FMLJavaModLoadingContext.get().getModEventBus());
		
		PVLogger.init(LogManager.getLogger());
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		
		MinecraftForge.EVENT_BUS.register(this);
		//MinecraftForge.EVENT_BUS.register(new GuiHandler());
		
	}
	
	private void setup(final FMLCommonSetupEvent event) {
		
		NetworkLoader.register();
		CapabilityManager.INSTANCE.register(IBAC.class, new BACStorage(), BAC::new);
		GuiHandler.initIcons();
		ModWorldGen.addFeatures();
		
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		
		RenderTypeLookup.setRenderLayer(ModBlocks.STEEL_BRAZIER.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(ModBlocks.CROP_AGAVE.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(ModBlocks.CINNAMON_LEAVES.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(ModBlocks.CINNAMON_SAPLING.get(), RenderType.getCutoutMipped());
		
		ScreenManager.registerFactory(ModContainers.STILL_MASTER.get(), StillMasterScreen::new);
		
		ModColors.init();
		
	}
	
	private void enqueueIMC(final InterModEnqueueEvent event) {
		
	}
	
	private void processIMC(final InterModProcessEvent event) {
		
	}
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		
	}
	
	@SubscribeEvent
	public void addStrippingWoodMechanic(PlayerInteractEvent.RightClickBlock event) {
		
		World world = event.getWorld();
		BlockPos blockpos = event.getPos();
		BlockState blockstate = world.getBlockState(blockpos);
		Block block = ModBlocks.BLOCK_STRIPPING_MAP.get(blockstate.getBlock());
		
		if (block != null) {
			
			PlayerEntity playerentity = event.getPlayer();
			world.playSound(playerentity, blockpos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
			
			if (!world.isRemote) {
				
				world.setBlockState(blockpos, block.getDefaultState().with(RotatedPillarBlock.AXIS, blockstate.get(RotatedPillarBlock.AXIS)), 11);
				
				if (playerentity != null) {
					
					event.getItemStack().damageItem(1, playerentity, (p_220040_1_) -> {
						
						p_220040_1_.sendBreakAnimation(event.getHand());
						
					});
					
					if (block == ModBlocks.CINNAMON_LOG.get()) {
						
						ResourceLocation resourcelocation = new ResourceLocation(Defines.MODID, "block_stripping/cinnamon_log");
						
						if (resourcelocation != LootTables.EMPTY) {
							
							LootContext lootContext = new LootContext.Builder((ServerWorld) world).withParameter(LootParameters.BLOCK_STATE, blockstate).build(LootParameterSets.BLOCK);
							ServerWorld serverworld = lootContext.getWorld();
							LootTable loottable = serverworld.getServer().getLootTableManager().getLootTableFromLocation(resourcelocation);
							
							for (ItemStack droppedItem : loottable.generate(lootContext)) {
								
								Block.spawnAsEntity(world, blockpos, droppedItem);
								
							}
							
						}
						
					}
					
				}
					
			}
			
		}
		
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