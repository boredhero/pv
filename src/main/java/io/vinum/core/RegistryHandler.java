package io.vinum.core;

import io.vinum.block.ModBlocks;
import io.vinum.barrel.ModDataComponents;
import io.vinum.fluid.ModFluids;
import io.vinum.capability.BACCapability;
import io.vinum.inventory.container.ModContainers;
import io.vinum.item.ModItemGroup;
import io.vinum.item.ModItems;
import io.vinum.tileentity.ModTileEntities;
import net.neoforged.bus.api.IEventBus;

public final class RegistryHandler {
    public static void registerDeferred(IEventBus bus) {
        ModDataComponents.COMPONENTS.register(bus);
        ModFluids.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItemGroup.TABS.register(bus);
        ModTileEntities.TILEENTITIES.register(bus);
        ModContainers.CONTAINERS.register(bus);
        BACCapability.ATTACHMENT_TYPES.register(bus);
    }

    private RegistryHandler() {}
}
