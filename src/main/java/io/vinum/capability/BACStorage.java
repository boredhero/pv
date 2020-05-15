package io.vinum.capability;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class BACStorage implements IStorage<IBAC> {
	
	@Override
	public INBT writeNBT(Capability<IBAC> capability, IBAC instance, Direction side) {
		
		return IntNBT.valueOf(instance.getBACLevel());
		
		//CompoundNBT tag = new CompoundNBT();
		//tag.putInt("BACLevel", instance.getBACLevel());
		
		//return tag;
		
	}

	@Override
	public void readNBT(Capability<IBAC> capability, IBAC instance, Direction side, INBT nbt) {
		
		//CompoundNBT tag = (CompoundNBT) nbt;
        //instance.setBACLevel(tag.getInt("BACLevel"));
		
		instance.setBACLevel(((IntNBT)nbt).getInt());
		
	}
	
}