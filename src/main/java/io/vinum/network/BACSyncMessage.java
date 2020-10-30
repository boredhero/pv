/*
    Project Vinum - BACSyncMessage.java
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
package io.vinum.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.network.NetworkEvent;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;

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