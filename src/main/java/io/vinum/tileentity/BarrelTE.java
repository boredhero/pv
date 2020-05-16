package io.vinum.tileentity;

import java.util.function.Supplier;

import io.vinum.block.ModBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BarrelTE extends TileEntity {

    public BarrelTE(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    @SubscribeEvent
    public static void registerTE(RegistryEvent.Register<TileEntityType<?>> evt) {
        //Do stuff ala https://mcforge.readthedocs.io/en/latest/tileentities/tileentity/#creating-a-tileentity
    }
    
}