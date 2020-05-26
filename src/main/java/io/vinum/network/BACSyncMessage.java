package io.vinum.network;

import java.util.function.Supplier;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.network.NetworkEvent;

public class BACSyncMessage {
	
	private CompoundNBT data;
	
	public BACSyncMessage(){}
	
	public BACSyncMessage(PacketBuffer buf) {
		
		this.data = buf.readCompoundTag();
		
	}

	public BACSyncMessage(CompoundNBT nbt) {
		
		this.data = nbt;
		
	}

	public void encode(PacketBuffer buf) {
		
		buf.writeCompoundTag(data);
		
	}
	
	public void handle(Supplier<NetworkEvent.Context> context) {
		
		context.get().enqueueWork(() -> {
			
			if (context.get().getDirection().getReceptionSide().isClient() && context.get().getDirection().getOriginationSide().isServer()) {
				
				ClientPlayerEntity player = Minecraft.getInstance().player;
				player.getCapability(BACCapability.BAC_CAPABILITY, null).ifPresent(state -> {
						
						Capability.IStorage<IBAC> storage = BACCapability.BAC_CAPABILITY.getStorage();
						storage.readNBT(BACCapability.BAC_CAPABILITY, state, null, data);
						
				});
				
			}
			
		});

		context.get().setPacketHandled(true);
		
	}
	
}