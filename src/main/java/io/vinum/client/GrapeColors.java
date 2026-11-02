package io.vinum.client;

import io.vinum.beverage.WineCatalog;
import io.vinum.block.GrapeCropBlock;
import io.vinum.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Shared neutral berry geometry takes its ripe skin color from the grape variety definition. */
@EventBusSubscriber(modid = "vinum", value = Dist.CLIENT)
public final class GrapeColors {
  private GrapeColors() {}

  @SubscribeEvent
  public static void colors(RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, level, pos, tint) -> {
          if (tint < 10) {
            return switch (tint) {
              // The leaf artwork is already green; modest neutral tints keep its veins visible.
              case 1 -> 0xFFDDE8D6;
              case 2 -> 0xFFF1FFE5;
              case 3 -> 0xFF9CAC58;
              case 4 -> 0xFFE1D5A1;
              default -> 0xFFFFFFFF;
            };
          }
          GrapeCropBlock crop = (GrapeCropBlock) state.getBlock();
          int ripe = WineCatalog.varieties().stream()
              .filter(variety -> variety.ingredient().equals(BuiltInRegistries.ITEM.getKey(crop.getVariety().grapeItem())))
              .map(WineCatalog.Variety::grapeColor).findFirst().orElse(0xFF729545);
          int age = crop.getAge(state);
          // Veraison is staggered: some berries change first, including white varieties turning gold.
          double ripeness = age >= 7 ? 1 : age == 6 ? (tint % 3 == 0 ? .82 : .38) : 0;
          int color = blend(0xFF7C9E43, ripe, ripeness);
          double shade = new double[] {.90, 1.05, .97, 1.13, .84}[(tint - 10) % 5];
          return shade(color, shade);
        },
        ModBlocks.CROP_GRAPE_CHARDONNAY.get(),
        ModBlocks.CROP_GRAPE_SAUVIGNON_BLANC.get(),
        ModBlocks.CROP_GRAPE_CABERNET_SAUVIGNON.get(),
        ModBlocks.CROP_GRAPE_MERLOT.get(),
        ModBlocks.CROP_GRAPE_PINOT_NOIR.get(),
        ModBlocks.CROP_GRAPE_ZINFANDEL.get());
  }

  private static int blend(int from, int to, double weight) {
    int result = 0xFF000000;
    for (int shift : new int[] {16, 8, 0}) {
      int first = (from >> shift) & 255;
      int second = (to >> shift) & 255;
      result |= (int) Math.round(first + (second - first) * weight) << shift;
    }
    return result;
  }

  private static int shade(int color, double multiplier) {
    int result = 0xFF000000;
    for (int shift : new int[] {16, 8, 0}) {
      result |= Math.min(255, (int) Math.round(((color >> shift) & 255) * multiplier)) << shift;
    }
    return result;
  }
}
