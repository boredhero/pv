/*
    Project Vinum - ProjectVinum.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package io.vinum;

import javax.annotation.Nonnull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
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
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;

import io.vinum.block.PVBlocks;
import io.vinum.block.stripping.PVStrippableBlocks;
import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.capability.BACStorage;
import io.vinum.capability.IBAC;
import io.vinum.client.renderer.color.PVColors;
import io.vinum.client.renderer.tileentity.PVSignTileEntityRenderer;
import io.vinum.common.PVDefines;
import io.vinum.config.PVConfig;
import io.vinum.core.PVRegistryHandler;
import io.vinum.fuels.PVFuels;
import io.vinum.gui.PVGUIHandler;
import io.vinum.gui.screen.inventory.StillMasterScreen;
import io.vinum.inventory.container.PVContainers;
import io.vinum.item.drinks.IDrink;
import io.vinum.network.BACSyncMessage;
import io.vinum.network.PVNetworkLoader;
import io.vinum.tileentity.PVTileEntities;
import io.vinum.tileentity.recipes.StillRecipes;
import io.vinum.worldgen.PVWorldGen;

@Mod(PVDefines.MODID)
public class ProjectVinum {
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final StillRecipes STILL_RECIPES = new StillRecipes();
	
	public ProjectVinum() {
		
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PVConfig.COMMON_SPEC);
		PVRegistryHandler.registerDeferred(FMLJavaModLoadingContext.get().getModEventBus());
		
		PVLogger.init(LogManager.getLogger());
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
		
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(PVFuels.instance);
		//MinecraftForge.EVENT_BUS.register(new GuiHandler());
		
	}
	
	private void setup(final FMLCommonSetupEvent event) {
		
		PVConfig.bakeConfig();
		PVNetworkLoader.register();
		CapabilityManager.INSTANCE.register(IBAC.class, new BACStorage(), BAC::new);
		PVGUIHandler.initIcons();
		PVWorldGen.addFeatures();
		PVStrippableBlocks.registerStrippableBlocks();
		STILL_RECIPES.initRecipes();
		
	}
	
	private void doClientStuff(final FMLClientSetupEvent event) {
		
		RenderTypeLookup.setRenderLayer(PVBlocks.STEEL_BRAZIER.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(PVBlocks.CROP_AGAVE.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(PVBlocks.CINNAMON_LEAVES.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(PVBlocks.CINNAMON_SAPLING.get(), RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(PVBlocks.CINNAMON_DOOR.get(), RenderType.getCutoutMipped());
		
		ScreenManager.registerFactory(PVContainers.STILL_MASTER.get(), StillMasterScreen::new);
		
		PVColors.init();
		
		ClientRegistry.bindTileEntityRenderer(PVTileEntities.VINUM_SIGN.get(), PVSignTileEntityRenderer::new);
		
	}
	
	private void enqueueIMC(final InterModEnqueueEvent event) {
		
	}
	
	private void processIMC(final InterModProcessEvent event) {
		
	}
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event) {
		
	}
	
	@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {
		
		@SubscribeEvent
		public static void registerSignSprites(TextureStitchEvent.Pre event) {
			
			if (event.getMap().getTextureLocation().equals(Atlases.SIGN_ATLAS)) {
				
				event.addSprite(new ResourceLocation(PVDefines.MODID, "entity/signs/cinnamon"));
				
			}
			
		}
		
	}
	
	@SubscribeEvent
	public void addStrippingWoodMechanic(PlayerInteractEvent.RightClickBlock event) {
		
		World world = event.getWorld();
		BlockPos blockPos = event.getPos();
		BlockState blockstate = world.getBlockState(blockPos);
		Block block = PVStrippableBlocks.BLOCK_STRIPPING_MAP.get(blockstate.getBlock());
		
		if (block != null && event.getItemStack().getItem() instanceof AxeItem) {
			
			PlayerEntity playerEntity = event.getPlayer();
			world.playSound(playerEntity, blockPos, SoundEvents.ITEM_AXE_STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
			
			if (!world.isRemote) {
				
				world.setBlockState(blockPos, block.getDefaultState().with(RotatedPillarBlock.AXIS, blockstate.get(RotatedPillarBlock.AXIS)), 11);
				
				if (playerEntity != null) {
					
					event.getItemStack().damageItem(1, playerEntity, (player) -> {
						
						player.sendBreakAnimation(event.getHand());
						
					});
					
				}
				
				if (block == PVBlocks.STRIPPED_CINNAMON_LOG.get() || block == PVBlocks.STRIPPED_CINNAMON_WOOD.get()) {
					
					ResourceLocation resourcelocation = new ResourceLocation(PVDefines.MODID, "block_stripping/cinnamon_log");
					
					if (resourcelocation != LootTables.EMPTY) {
						
						LootContext lootContext = new LootContext.Builder((ServerWorld) world).withParameter(LootParameters.POSITION, blockPos).withParameter(LootParameters.TOOL, event.getItemStack()).withParameter(LootParameters.BLOCK_STATE, blockstate).build(LootParameterSets.BLOCK);
						ServerWorld serverworld = lootContext.getWorld();
						LootTable loottable = serverworld.getServer().getLootTableManager().getLootTableFromLocation(resourcelocation);
						
						for (ItemStack droppedItem : loottable.generate(lootContext)) {
							
							Block.spawnAsEntity(world, blockPos, droppedItem);
							
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
	@SubscribeEvent
	public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		
		if (event.getObject() instanceof PlayerEntity) {
			
			event.addCapability(new ResourceLocation(PVDefines.MODID, "bac_capability"), new BACCapability());
			
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
                    PVNetworkLoader.INSTANCE.send(PacketDistributor.PLAYER.with(() -> target), new BACSyncMessage(nbt));
                    
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