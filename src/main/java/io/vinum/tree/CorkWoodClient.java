package io.vinum.tree;

import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = "vinum", value = Dist.CLIENT)
public final class CorkWoodClient {
  private CorkWoodClient() {}

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent event) {
    event.enqueueWork(
        () ->
            ItemProperties.register(
                CorkOakContent.LOG_ITEM.get(),
                CorkOakContent.id("cork_harvested"),
                (stack, level, entity, seed) -> CorkLogItem.harvested(stack) ? 1 : 0));
  }
}
