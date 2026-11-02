package io.vinum.machine;

import net.minecraft.client.gui.GuiGraphics;

/** Shared timber-and-brass menu framing, independent of any machine's processing logic. */
final class MachineScreenDrawing {
  private MachineScreenDrawing() {}

  static void panel(GuiGraphics g, int x, int y, int width, int height) {
    g.fill(x, y, x + width, y + height, 0xFF44362A);
    g.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFAA8860);
    g.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFFE1D2B8);
    for (int dx : new int[] {3, width - 5})
      for (int dy : new int[] {3, height - 5})
        g.fill(x + dx, y + dy, x + dx + 2, y + dy + 2, 0xFFE0B85B);
  }

  static void slot(GuiGraphics g, int x, int y) {
    g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF65523F);
    g.fill(x, y, x + 16, y + 16, 0xFFB7A58B);
  }

  static void bar(
      GuiGraphics g, int x, int y, int width, int height, int amount, int capacity, int color) {
    g.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF65523F);
    g.fill(x, y, x + width, y + height, 0xFF302B29);
    int filled =
        Math.clamp((int) ((long) Math.max(0, amount) * height / Math.max(1, capacity)), 0, height);
    g.fill(x, y + height - filled, x + width, y + height, color | 0xFF000000);
  }
}
