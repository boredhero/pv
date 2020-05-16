package io.vinum.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;

import io.vinum.common.Defines;
import io.vinum.inventory.container.StillMasterContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.AbstractFurnaceContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StillMasterScreen extends ContainerScreen<StillMasterContainer> implements IContainerListener {
	
	private static final ResourceLocation STILL_MASTER_RESOURCE = new ResourceLocation(Defines.MODID, "textures/gui/container/still.png");
	
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
	
	@Override
	public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
		
	}
	
	@Override
	public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
		
	}
	
	@Override
	public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
		
	}
	
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		
		RenderSystem.disableBlend();
		this.font.drawString(this.title.getFormattedText(), 60.0F, 6.0F, 4210752);

	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bindTexture(STILL_MASTER_RESOURCE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(i, j, 0, 0, this.xSize, this.ySize);
		
		int k = ((StillMasterContainer)this.container).getBurnLeftScaled();
        this.blit(i + 56, j + 36 + 12 - k, 176, 12 - k, 14, k + 1);
		
        int l = ((StillMasterContainer)this.container).getCookProgressionScaled();
        this.blit(i + 79, j + 34, 176, 14, l + 1, 16);
        
	}

}