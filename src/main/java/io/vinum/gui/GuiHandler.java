package io.vinum.gui;

import java.util.ArrayList;

import javax.annotation.Nullable;

import io.vinum.common.Defines;
import io.yooksi.cocolib.gui.Alignment;
import io.yooksi.cocolib.gui.GuiElement;
import io.yooksi.cocolib.gui.SpriteObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GuiHandler {
	
	public static final ArrayList<SpriteObject> BAC_HUD_EMPTY_ICONS = new ArrayList<SpriteObject>();
	public static final ArrayList<SpriteObject> BAC_HUD_FULL_ICONS = new ArrayList<SpriteObject>();
	//BAC_HUD_EMPTY = SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hub_bar.png").withPos(Alignment.BOTTOM_RIGHT, 307, 239).withSize(9, 9).build();
	
	public static final void initIcons() {
		
		for (int i = 0; i < 9; i++) {
			
			BAC_HUD_EMPTY_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hub_bar.png").withPos(Alignment.BOTTOM_RIGHT, 223 + (i * 9) + (i * 3), 191).withUV(0, 0).withSize(9, 9).build());
			BAC_HUD_FULL_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hub_bar.png").withPos(Alignment.BOTTOM_RIGHT, 223 + (i * 9) + (i * 3), 191).withUV(9, 0).withSize(9, 9).build());
			
		}
		
	}
	
	@SubscribeEvent
	public void onPreRenderOverlay(RenderGameOverlayEvent.Pre event) {
		
		@Nullable PlayerController controller = Minecraft.getInstance().playerController;
        
        if (controller != null && controller.gameIsSurvivalOrAdventure()) {
        	
        	for (int i = 0; i < BAC_HUD_EMPTY_ICONS.size(); i++) {
        		
        		//GuiElement.bindAndDrawTexture(BAC_HUD_EMPTY_ICONS.get(i));
        		
        	}
        	
        	for (SpriteObject full_bac_icon : BAC_HUD_FULL_ICONS) {
        		
        		//GuiElement.bindAndDrawTexture(full_bac_icon);
        		
        	}
        	
        }
		
	}
	
}