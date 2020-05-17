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