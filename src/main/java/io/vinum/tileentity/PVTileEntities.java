/*
    Project Vinum - PVTileEntities.java
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

import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import io.vinum.block.PVBlocks;
import io.vinum.common.PVDefines;

public class PVTileEntities {
	
	public static final DeferredRegister<TileEntityType<?>> TILEENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, PVDefines.MODID);
	
	public static final RegistryObject<TileEntityType<?>> STILL_MASTER = TILEENTITIES.register("still_master", () -> TileEntityType.Builder.of(StillMasterTileEntity::new, PVBlocks.STILL_MULTIBLOCK_PART_1.get()).build(null));
	public static final RegistryObject<TileEntityType<?>> STILL_SLAVE = TILEENTITIES.register("still_slave", () -> TileEntityType.Builder.of(StillSlaveTileEntity::new, PVBlocks.STILL_MULTIBLOCK_PART_2.get(), PVBlocks.STILL_MULTIBLOCK_PART_3.get(), PVBlocks.STILL_MULTIBLOCK_PART_4.get()).build(null));
	
	public static final RegistryObject<TileEntityType<?>> BARREL = TILEENTITIES.register("barrel", () -> TileEntityType.Builder.of(BarrelTileEntity::new, PVBlocks.STEEL_POT.get()).build(null));
	
	public static final RegistryObject<TileEntityType<PVSignTileEntity>> VINUM_SIGN = TILEENTITIES.register("vinum_sign", () -> TileEntityType.Builder.of(PVSignTileEntity::new, PVBlocks.CINNAMON_SIGN.get(), PVBlocks.CINNAMON_WALL_SIGN.get()).build(null));
	
}