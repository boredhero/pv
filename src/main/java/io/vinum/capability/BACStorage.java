/*
    Project Vinum - BACStorage.java
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
package io.vinum.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class BACStorage implements IStorage<IBAC> {
	
	@Override
	public INBT writeNBT(Capability<IBAC> capability, IBAC instance, Direction side) {
		
		CompoundNBT tag = new CompoundNBT();
		tag.putDouble("BACLevel", instance.getBACLevel());
		tag.putInt("BACTicks", instance.getBACTicks());
		
		return tag;
		
	}

	@Override
	public void readNBT(Capability<IBAC> capability, IBAC instance, Direction side, INBT nbt) {
		
		CompoundNBT tag = (CompoundNBT) nbt;
		instance.setBACLevel(tag.getDouble("BACLevel"));
		instance.setBACTicks(tag.getInt("BACTicks"));
		
	}
	
}