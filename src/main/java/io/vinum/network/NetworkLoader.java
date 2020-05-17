package io.vinum.network;

import io.vinum.common.Defines;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkLoader {
	
	private static int channelID = 1;
	
	private static final String PROTOCOL_VERSION = "1";
	
	public static SimpleChannel INSTANCE;
	
	public static void register() {
		
		INSTANCE = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Defines.MODID, "main")).clientAcceptedVersions(PROTOCOL_VERSION::equals).serverAcceptedVersions(PROTOCOL_VERSION::equals).networkProtocolVersion(() -> PROTOCOL_VERSION).simpleChannel();
		INSTANCE.messageBuilder(BACSyncMessage.class, channelID++).decoder(BACSyncMessage::new).encoder(BACSyncMessage::encode).consumer(BACSyncMessage::handle).add();
		
	}
	
}