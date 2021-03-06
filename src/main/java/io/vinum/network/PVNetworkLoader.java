/*
    Project Vinum - PVNetworkLoader.java
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

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import io.vinum.common.PVDefines;

public class PVNetworkLoader {
	
	private static int channelID = 1;
	
	private static final String PROTOCOL_VERSION = "1";
	
	public static SimpleChannel INSTANCE;
	
	public static void register() {
		
		INSTANCE = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(PVDefines.MODID, "main")).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).networkProtocolVersion(() -> PROTOCOL_VERSION).simpleChannel();
		INSTANCE.messageBuilder(BACSyncMessage.class, channelID++).decoder(BACSyncMessage::new).encoder(BACSyncMessage::encode).consumer(BACSyncMessage::handle).add();
		
	}
	
}