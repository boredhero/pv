package io.vinum.tileentity;

import io.vinum.block.ModBlocks;
import io.vinum.common.Defines;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Defines.MODID)
public final class ModTileEntities {
    public static final DeferredRegister<BlockEntityType<?>> TILEENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Defines.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StillMasterTileEntity>> STILL_MASTER =
            TILEENTITIES.register("still_master", () -> BlockEntityType.Builder.of(
                    StillMasterTileEntity::new, ModBlocks.STILL_MULTIBLOCK_PART_1.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BarrelBlockEntity>> BARREL =
            TILEENTITIES.register("barrel", () -> BlockEntityType.Builder.of(BarrelBlockEntity::new,
                    ModBlocks.BARREL.get(), ModBlocks.BLOCK_WINE_BARREL_EMPTY.get(),
                    ModBlocks.BLOCK_WINE_BARREL_CHARDONNAY.get(), ModBlocks.BLOCK_WINE_BARREL_SAUVIGNON_BLANC.get(),
                    ModBlocks.BLOCK_WINE_BARREL_CABERNET_SAUVIGNON.get(), ModBlocks.BLOCK_WINE_BARREL_MERLOT.get(),
                    ModBlocks.BLOCK_WINE_BARREL_PINOT_NOIR.get(), ModBlocks.BLOCK_WINE_BARREL_ZINFANDEL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermentationVesselBlockEntity>> CARBOY =
            TILEENTITIES.register("carboy", () -> BlockEntityType.Builder.of(FermentationVesselBlockEntity::new, ModBlocks.CARBOY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermentationVesselBlockEntity>> FERMENTATION_VAT =
            TILEENTITIES.register("fermentation_vat", () -> BlockEntityType.Builder.of(FermentationVesselBlockEntity::new, ModBlocks.FERMENTATION_VAT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.vinum.machine.WinePressBlockEntity>> WINE_PRESS = TILEENTITIES.register("wine_press", () -> BlockEntityType.Builder.of(io.vinum.machine.WinePressBlockEntity::new, ModBlocks.WINE_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<io.vinum.machine.BasicGeneratorBlockEntity>> BASIC_GENERATOR = TILEENTITIES.register("basic_generator", () -> BlockEntityType.Builder.of(io.vinum.machine.BasicGeneratorBlockEntity::new, ModBlocks.BASIC_GENERATOR.get()).build(null));

    private ModTileEntities() {}

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level,pos,state,entity,side) -> {
            var base=io.vinum.machine.MachineBlock.base(state,pos);
            return level.getBlockEntity(base) instanceof io.vinum.machine.WinePressBlockEntity press ? press.items(side) : null;
        }, ModBlocks.WINE_PRESS.get());
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level,pos,state,entity,side) -> {
            var base=io.vinum.machine.MachineBlock.base(state,pos);
            return level.getBlockEntity(base) instanceof io.vinum.machine.WinePressBlockEntity press ? press.fluids() : null;
        }, ModBlocks.WINE_PRESS.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level,pos,state,entity,side) -> {
            var base=io.vinum.machine.MachineBlock.base(state,pos);
            return level.getBlockEntity(base) instanceof io.vinum.machine.WinePressBlockEntity press && press.hasMotor() ? press.energy : null;
        }, ModBlocks.WINE_PRESS.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BASIC_GENERATOR.get(), io.vinum.machine.BasicGeneratorBlockEntity::items);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BASIC_GENERATOR.get(), (generator, side) -> generator.energy);
        io.vinum.item.ModItems.ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof io.vinum.item.FluidContainerItem container)
                event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> container.handler(stack), entry.get());
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STILL_MASTER.get(),
                StillMasterTileEntity::getItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, CARBOY.get(), FermentationVesselBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FERMENTATION_VAT.get(), FermentationVesselBlockEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STILL_MASTER.get(), StillMasterTileEntity::getFluidHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BARREL.get(),
                BarrelBlockEntity::getFluidHandler);
    }
}
