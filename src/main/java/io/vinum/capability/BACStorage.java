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