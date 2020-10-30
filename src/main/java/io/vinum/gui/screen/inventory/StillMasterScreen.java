/*
    Project Vinum - StillMasterScreen.java
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
package io.vinum.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import io.vinum.common.PVDefines;
import io.vinum.inventory.container.StillMasterContainer;

@OnlyIn(Dist.CLIENT)
public class StillMasterScreen extends ContainerScreen<StillMasterContainer> implements IContainerListener {
	
	private static final ResourceLocation STILL_MASTER_RESOURCE = new ResourceLocation(PVDefines.MODID, "textures/gui/container/still.png");
	
	public StillMasterScreen(StillMasterContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
		super(screenContainer, inv, titleIn);
		
	}
	
	protected void init() {
		super.init();
		
		this.container.addListener(this);
		
	}
	
	public void resize(Minecraft minecraft, int width, int height) {
		
		this.init(minecraft, width, height);
	
	}
	
	public void tick() {
		super.tick();
		
	}
	
	@Override
	public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
		
	}
	
	@Override
	public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
		
	}
	
	@Override
	public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
		
	}
	
	public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
		
		this.renderBackground();
		super.render(p_render_1_, p_render_2_, p_render_3_);
		RenderSystem.disableBlend();
		this.renderHoveredToolTip(p_render_1_, p_render_2_);
		
	}
	
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		
		RenderSystem.disableBlend();
		this.font.drawString(this.title.getFormattedText(), 70.0F, 6.0F, 4210752);

	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(STILL_MASTER_RESOURCE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(i, j, 0, 0, this.xSize, this.ySize);
		
		int k = ((StillMasterContainer)this.container).getBurnLeftScaled();
		
		if (k != 0) {
			
			this.blit(i + 43, j + 48 - k, 176, 12 - k, 14, k + 1);
			
		}
		
        int l = ((StillMasterContainer)this.container).getCookProgressionScaled();
        
        if (l != 0) {
        	
        	this.blit(i + 60, j + 19, 176, 14, l + 1, 16);
        	
        }
        
	}

}