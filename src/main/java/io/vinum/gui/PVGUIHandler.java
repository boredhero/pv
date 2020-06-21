/*
    Project Vinum - PVGUIHandler.java
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
package io.vinum.gui;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import io.vinum.common.PVDefines;
import io.yooksi.cocolib.gui.Alignment;
import io.yooksi.cocolib.gui.GuiElement;
import io.yooksi.cocolib.gui.SpriteObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.NonNullConsumer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PVGUIHandler {
	
	public static boolean hasInit;
	public static boolean hasSwimmingInit;
	
	public static final ArrayList<SpriteObject> BAC_HUD_EMPTY_ICONS = new ArrayList<SpriteObject>();
	public static final ArrayList<SpriteObject> BAC_HUD_FULL_ICONS = new ArrayList<SpriteObject>();
	
	public static final void initIcons() {
		
		BAC_HUD_EMPTY_ICONS.clear();
		BAC_HUD_FULL_ICONS.clear();
		
		for (int i = 0; i < 9; i++) {
			
			BAC_HUD_EMPTY_ICONS.add(SpriteObject.Builder.create(PVDefines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 40).withUV(0, 0).withSize(9, 9).build());
			BAC_HUD_FULL_ICONS.add(SpriteObject.Builder.create(PVDefines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 40).withUV(9, 0).withSize(9, 9).build());
			
		}
		
		hasInit = true;
		hasSwimmingInit = false;
		
	}
	
	public static final void initIconsForSwimming() {
		
		BAC_HUD_EMPTY_ICONS.clear();
		BAC_HUD_FULL_ICONS.clear();
		
		for (int i = 0; i < 9; i++) {
			
			BAC_HUD_EMPTY_ICONS.add(SpriteObject.Builder.create(PVDefines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 50).withUV(0, 0).withSize(9, 9).build());
			BAC_HUD_FULL_ICONS.add(SpriteObject.Builder.create(PVDefines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 50).withUV(9, 0).withSize(9, 9).build());
			
		}
		
		hasSwimmingInit = true;
		hasInit = false;
		
	}
	
	@SubscribeEvent
	public void onPreRenderOverlay(RenderGameOverlayEvent.Pre event) {
		
		@Nullable PlayerController controller = Minecraft.getInstance().playerController;
		
		@Nullable ClientPlayerEntity player = Minecraft.getInstance().player;
		
		if (controller != null && player != null && controller.gameIsSurvivalOrAdventure()) {
			
			player.getCapability(BACCapability.BAC_CAPABILITY).ifPresent(new NonNullConsumer<IBAC>() {
				
				@Override
				public void accept(@Nonnull IBAC iBAC) {
					
					if (iBAC.getBACLevel() > 0) {
						
						if (player.getAir() > 0 && player.getAir() != 300 && !hasSwimmingInit) {
							
							initIconsForSwimming();
							
						} else if (!hasInit) {
							
							initIcons();
							
						}
						
						for (SpriteObject empty_bac_icon : BAC_HUD_EMPTY_ICONS) {
							
							GuiElement.bindAndDrawTexture(empty_bac_icon);
							
						}
						
						for (int i = 0; i < BAC_HUD_FULL_ICONS.size(); i++) {
							
							if (i + 1 <= iBAC.getBACLevel()) {
								
								GuiElement.bindAndDrawTexture(BAC_HUD_FULL_ICONS.get(i));
								
							}
							
						}
						
					}
					
				}
				
			});
			
		}
		
	}
	
}