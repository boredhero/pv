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
	public static final RegistryObject<TileEntityType<?>> STILL_SLAVE = TILEENTITIES.register("still_slave", () -> TileEntityType.Builder.create(StillSlaveTileEntity::new, ModBlocks.STILL_MULTIBLOCK_PART_2.get(), ModBlocks.STILL_MULTIBLOCK_PART_3.get(), ModBlocks.STILL_MULTIBLOCK_PART_4.get()).build(null));
	
	public static final RegistryObject<TileEntityType<?>> BARREL = TILEENTITIES.register("barrel", () -> TileEntityType.Builder.create(BarrelTileEntity::new, ModBlocks.STEEL_POT.get()).build(null));
	
	public static final RegistryObject<TileEntityType<ModSignTileEntity>> VINUM_SIGN = TILEENTITIES.register("vinum_sign", () -> TileEntityType.Builder.create(ModSignTileEntity::new, ModBlocks.CINNAMON_SIGN.get(), ModBlocks.CINNAMON_WALL_SIGN.get()).build(null));
	
}