package io.vinum.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WinePressScreen extends AbstractContainerScreen<WinePressMenu> {
  private net.minecraft.client.gui.components.Button purge;

  public WinePressScreen(WinePressMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageHeight = 208;
    inventoryLabelY = 115;
  }

  @Override
  protected void init() {
    super.init();
    purge = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
        Component.translatable("gui.vinum.purge"), button -> {
          if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, WinePressMenu.PURGE_BUTTON);
        }).bounds(leftPos + 116, topPos + 84, 50, 18)
        .tooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("gui.vinum.purge_hint"))).build());
    purge.active = menu.juice() > 0;
  }

  @Override
  protected void containerTick() {
    super.containerTick();
    purge.active = menu.juice() > 0;
  }

  @Override
  protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    MachineScreenDrawing.panel(g, leftPos, topPos, imageWidth, imageHeight);
    for (var slot : menu.slots) MachineScreenDrawing.slot(g, leftPos + slot.x, topPos + slot.y);
    if (menu.hasMotor())
      MachineScreenDrawing.bar(
          g,
          leftPos + 9,
          topPos + 24,
          10,
          56,
          menu.energy(),
          WinePressBlockEntity.ENERGY_CAPACITY,
          0xFFE3B75C);
    MachineScreenDrawing.bar(
        g,
        leftPos + 149,
        topPos + 24,
        12,
        56,
        menu.juice(),
        WinePressBlockEntity.TANK_CAPACITY,
        menu.color());
    g.fill(leftPos + 65, topPos + 38, leftPos + 99, topPos + 50, 0xFF4C4035);
    int width =
        menu.required() == 0 ? 0 : Math.clamp(menu.progress() * 32 / menu.required(), 0, 32);
    g.fill(leftPos + 66, topPos + 39, leftPos + 66 + width, topPos + 49, 0xFF9BB867);
    int percent = menu.required() == 0 ? 0 : menu.progress() * 100 / menu.required();
    g.drawCenteredString(font, percent + "%", leftPos + 82, topPos + 55, 0xFF493B2D);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    super.renderLabels(g, mouseX, mouseY);
    g.drawString(font, Component.translatable("gui.vinum.press_input"), 30, 24, 0x493B2D, false);
    g.drawString(font, Component.translatable("gui.vinum.container"), 104, 12, 0x493B2D, false);
    g.drawString(font, Component.translatable("gui.vinum.output"), 106, 54, 0x493B2D, false);
    g.drawString(
        font,
        Component.translatable(
            menu.hasMotor()
                ? "gui.vinum.motor_mode"
                : menu.hasCrank() ? "gui.vinum.manual_mode" : "gui.vinum.attach_crank"),
        8,
        105,
        0x493B2D,
        false);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    super.render(g, mouseX, mouseY, partialTick);
    renderTooltip(g, mouseX, mouseY);
    if (menu.hasMotor() && isHovering(9, 24, 10, 56, mouseX, mouseY))
      g.renderTooltip(
          font,
          Component.translatable(
              "gui.vinum.energy", menu.energy(), WinePressBlockEntity.ENERGY_CAPACITY),
          mouseX,
          mouseY);
    if (isHovering(149, 24, 12, 56, mouseX, mouseY))
      g.renderComponentTooltip(
          font,
          java.util.List.of(
              menu.fluidName(),
              Component.translatable(
                  "tooltip.vinum.container_capacity",
                  menu.juice(),
                  WinePressBlockEntity.TANK_CAPACITY)),
          mouseX,
          mouseY);
  }
}
