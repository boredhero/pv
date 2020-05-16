package io.vinum.tileentity;

import io.vinum.block.ModBlocks;
import io.vinum.common.Defines;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModTileEntities {
	
	public static final DeferredRegister<TileEntityType<?>> TILEENTITIES = new DeferredRegister<>(ForgeRegistries.TILE_ENTITIES, Defines.MODID);
	
	public static final RegistryObject<TileEntityType<?>> STILL_MASTER = TILEENTITIES.register("still_master", () -> TileEntityType.Builder.create(StillMasterTileEntity::new, ModBlocks.STILL_MULTIBLOCK_PART_1.get()).build(null));
	
}