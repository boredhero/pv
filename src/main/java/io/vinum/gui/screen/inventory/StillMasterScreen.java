package io.vinum.gui.screen.inventory;

import io.vinum.common.Defines;
import io.vinum.tileentity.StillMasterTileEntity;
import io.vinum.inventory.container.StillMasterContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StillMasterScreen extends AbstractContainerScreen<StillMasterContainer> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Defines.MODID, "textures/gui/container/still.png");

    public StillMasterScreen(StillMasterContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 22 && mouseY >= topPos + 18 && mouseY < topPos + 66) {
            graphics.renderTooltip(font, Component.translatable("tooltip.vinum.still_wash", menu.getWashAmount(), menu.getRequiredWashAmount()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.fill(leftPos + 8, topPos + 18, leftPos + 22, topPos + 66, 0xFF333333);
        int washHeight = menu.getWashAmount() * 46 / StillMasterTileEntity.WASH_CAPACITY;
        graphics.fill(leftPos + 9, topPos + 65 - washHeight, leftPos + 21, topPos + 65, 0xFFC6B776);
        int heat = menu.getBurnLeftScaled();
        if (heat > 0) {
            graphics.blit(TEXTURE, leftPos + 43, topPos + 36 + 13 - heat, 176, 13 - heat, 14, heat);
        }
        int progress = menu.getCookProgressionScaled();
        if (progress > 0) {
            graphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, progress, 16);
        }
    }
}
