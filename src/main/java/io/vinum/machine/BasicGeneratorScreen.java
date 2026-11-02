package io.vinum.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BasicGeneratorScreen extends AbstractContainerScreen<BasicGeneratorMenu> {
  public BasicGeneratorScreen(BasicGeneratorMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    MachineScreenDrawing.panel(g, leftPos, topPos, imageWidth, imageHeight);
    for (var slot : menu.slots) MachineScreenDrawing.slot(g, leftPos + slot.x, topPos + slot.y);
    MachineScreenDrawing.bar(
        g,
        leftPos + 80,
        topPos + 24,
        12,
        40,
        menu.energy(),
        BasicGeneratorBlockEntity.CAPACITY,
        0xFFE3B75C);
    int burn =
        menu.burnDuration() == 0
            ? 0
            : Math.clamp((int) ((long) menu.burnRemaining() * 14 / menu.burnDuration()), 0, 14);
    g.fill(leftPos + 45, topPos + 69 - burn, leftPos + 59, topPos + 69, 0xFFE98434);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    super.renderLabels(g, mouseX, mouseY);
    g.drawString(font, Component.translatable("gui.vinum.fuel"), 42, 24, 0x493B2D, false);
    g.drawString(font, Component.translatable("gui.vinum.remainder"), 106, 24, 0x493B2D, false);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    super.render(g, mouseX, mouseY, partialTick);
    renderTooltip(g, mouseX, mouseY);
    if (isHovering(80, 24, 12, 40, mouseX, mouseY))
      g.renderTooltip(
          font,
          Component.translatable(
              "gui.vinum.energy", menu.energy(), BasicGeneratorBlockEntity.CAPACITY),
          mouseX,
          mouseY);
  }
}
