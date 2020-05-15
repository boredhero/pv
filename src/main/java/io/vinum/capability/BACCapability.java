package io.vinum.capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class BACCapability implements ICapabilitySerializable<IntNBT> {
	
	@CapabilityInject(IBAC.class)
	public static final Capability<IBAC> BAC_CAPABILITY = null;
	private LazyOptional<IBAC> instance = LazyOptional.of(BAC_CAPABILITY::getDefaultInstance);
	
	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		
		return BAC_CAPABILITY.orEmpty(cap, instance);
		
	}
	
	@Override
	public IntNBT serializeNBT() {
		
		return (IntNBT) BAC_CAPABILITY.getStorage().writeNBT(BAC_CAPABILITY, instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional cannot be empty!")), null);
		
	}
	
	@Override
	public void deserializeNBT(IntNBT nbt) {
		
		BAC_CAPABILITY.getStorage().readNBT(BAC_CAPABILITY, instance.orElseThrow(() -> new IllegalArgumentException("LazyOptional cannot be empty!")), null, nbt);
		
	}
	
}