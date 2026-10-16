package io.vinum.tileentity;

import io.vinum.block.ModBlocks;
import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Defines.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModTileEntities {
    public static final DeferredRegister<BlockEntityType<?>> TILEENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Defines.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StillMasterTileEntity>> STILL_MASTER =
            TILEENTITIES.register("still_master", () -> BlockEntityType.Builder.of(
                    StillMasterTileEntity::new, ModBlocks.STILL_MULTIBLOCK_PART_1.get()).build(null));

    private ModTileEntities() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STILL_MASTER.get(),
                StillMasterTileEntity::getItemHandler);
    }
}
