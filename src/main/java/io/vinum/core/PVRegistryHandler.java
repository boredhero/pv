/*
    Project Vinum - PVRegistryHandler.java
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
package io.vinum.core;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.ObjectHolder;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import io.vinum.PVLogger;
import io.vinum.block.PVBlocks;
import io.vinum.block.stripping.PVStrippableBlocks;
import io.vinum.common.PVDefines;
import io.vinum.inventory.container.PVContainers;
import io.vinum.item.PVItemGroup;
import io.vinum.item.PVItems;
import io.vinum.tileentity.PVTileEntities;
import io.vinum.util.PVUtil;
import io.vinum.worldgen.PVFeatures;

@ObjectHolder(PVDefines.MODID)
@Mod.EventBusSubscriber(modid = PVDefines.MODID, bus=Mod.EventBusSubscriber.Bus.MOD)
public class PVRegistryHandler {


	@SubscribeEvent
	public static void onRegisterItems(final RegistryEvent.Register<Item> event) {

		final IForgeRegistry<Item> registry = event.getRegistry();
		AtomicInteger blockItemCount = new AtomicInteger();

		PVBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)
				.filter(PVBlocks::needsItemBlock).forEach(block -> {

					final Item.Properties properties = new Item.Properties().group(PVItemGroup.MAIN);
					final BlockItem blockItem = new BlockItem(block, properties);
					blockItem.setRegistryName(Objects.requireNonNull(block.getRegistryName()));
					registry.register(blockItem);
					blockItemCount.getAndIncrement();
				});

		PVLogger.debug("Registered %d BlockItems", blockItemCount.get());
	}

	public static void registerDeferred(IEventBus iEventBus) {

		PVBlocks.BLOCKS.register(iEventBus);
		PVItems.ITEMS.register(iEventBus);
		PVTileEntities.TILEENTITIES.register(iEventBus);
		PVContainers.CONTAINERS.register(iEventBus);
		PVFeatures.FEATURES.register(iEventBus);
		
	}

	/**
	 * Prepare given registry entry for registration process.
	 *
	 * @param entry registry entry to process.
	 * @param name path to use for registry name creation.
	 * @param <T> Forge registry entry type
	 *
	 * @return given registry entry
	 */
	@SuppressWarnings("unused")
	private static <T extends IForgeRegistryEntry<T>> T setup(final T entry, final String name) {

		entry.setRegistryName(PVUtil.getModResourceLocation(name));
		return entry;
	}
}