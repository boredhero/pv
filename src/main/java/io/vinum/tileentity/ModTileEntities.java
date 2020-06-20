/*
    Project Vinum - ModTileEntities.java
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