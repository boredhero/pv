package io.vinum.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class BACStorage implements IStorage<IBAC> {
	
	public static final BACStorage INSTACE = new BACStorage();
	
	@Override
	public INBT writeNBT(Capability<IBAC> capability, IBAC instance, Direction side) {
		
		CompoundNBT tag = new CompoundNBT();
		tag.putInt("BACLevel", instance.getBACLevel());
		
		return tag;
		
	}

	@Override
	public void readNBT(Capability<IBAC> capability, IBAC instance, Direction side, INBT nbt) {
		
		CompoundNBT tag = (CompoundNBT) nbt;
        instance.setBACLevel(tag.getInt("BACLevel"));
		
	}
	
}