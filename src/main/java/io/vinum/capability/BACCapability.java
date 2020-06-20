/*
    Project Vinum - BACCapability.java
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class BACCapability implements ICapabilitySerializable<INBT> {
	
	@CapabilityInject(IBAC.class)
	public static final Capability<IBAC> BAC_CAPABILITY = null;
	private LazyOptional<IBAC> instance = LazyOptional.of(BAC_CAPABILITY::getDefaultInstance);
	
	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		
		return BAC_CAPABILITY.orEmpty(cap, instance);
		
	}
	
	@Override
	public INBT serializeNBT() {
		
		return BAC_CAPABILITY.getStorage().writeNBT(BAC_CAPABILITY, instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional cannot be empty!")), null);
		
	}
	
	@Override
	public void deserializeNBT(INBT nbt) {
		
		BAC_CAPABILITY.getStorage().readNBT(BAC_CAPABILITY, instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional cannot be empty!")), null, nbt);
		
	}
	
}