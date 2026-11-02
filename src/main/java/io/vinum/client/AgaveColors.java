package io.vinum.client;

import io.vinum.block.ModBlocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Blue-gray leaf faces preserve the reference plant's waxy blades, with pale raised edges. */
@EventBusSubscriber(modid = "vinum", value = Dist.CLIENT)
public final class AgaveColors {
  private AgaveColors() {}

  @SubscribeEvent
  public static void colors(RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, level, pos, tint) ->
            switch (tint) {
              case 1 -> 0xFF678E84;
              case 2 -> 0xFF507D79;
              case 3 -> 0xFFA1B7A6;
              case 4 -> 0xFF365D5A;
              default -> 0xFF739C94;
            },
        ModBlocks.CROP_AGAVE.get(),
        ModBlocks.AGAVE_LEAVES.get());
  }
}
