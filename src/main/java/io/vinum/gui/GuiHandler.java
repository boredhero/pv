package io.vinum.gui;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.vinum.capability.BACCapability;
import io.vinum.capability.IBAC;
import io.vinum.common.Defines;
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
public class GuiHandler {
	
	public static boolean hasInit;
	public static boolean hasSwimmingInit;
	
	public static final ArrayList<SpriteObject> BAC_HUD_EMPTY_ICONS = new ArrayList<SpriteObject>();
	public static final ArrayList<SpriteObject> BAC_HUD_FULL_ICONS = new ArrayList<SpriteObject>();
	
	public static final void initIcons() {
		
		BAC_HUD_EMPTY_ICONS.clear();
		BAC_HUD_FULL_ICONS.clear();
		
		for (int i = 0; i < 9; i++) {
			
			BAC_HUD_EMPTY_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 40).withUV(0, 0).withSize(9, 9).build());
			BAC_HUD_FULL_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 40).withUV(9, 0).withSize(9, 9).build());
			
		}
		
		hasInit = true;
		hasSwimmingInit = false;
		
	}
	
	public static final void initIconsForSwimming() {
		
		BAC_HUD_EMPTY_ICONS.clear();
		BAC_HUD_FULL_ICONS.clear();
		
		for (int i = 0; i < 9; i++) {
			
			BAC_HUD_EMPTY_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 50).withUV(0, 0).withSize(9, 9).build());
			BAC_HUD_FULL_ICONS.add(SpriteObject.Builder.create(Defines.MODID, "textures/gui/bac_hud_bar.png").withPos(Alignment.BOTTOM_CENTER, 13 + (i * 9), 50).withUV(9, 0).withSize(9, 9).build());
			
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