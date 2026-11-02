package io.vinum.client;

import io.vinum.beverage.WineCatalog;
import io.vinum.common.Defines;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.production.ProductionInputs;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Defines.MODID, value = Dist.CLIENT)
public final class BeverageItemClient {
    private BeverageItemClient() {}
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event) {
        ModItems.ITEMS.getEntries().forEach(entry -> event.register((stack, tint) -> {
            if (tint != 0) return 0xFFFFFFFF;
            var fluid = stack.getItem() instanceof FluidContainerItem container ? container.contents(stack) : ProductionInputs.fromItem(stack);
            return WineCatalog.color(fluid);
        }, entry.get()));
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModItems.ITEMS.getEntries().forEach(entry -> {
            if (entry.get() instanceof FluidContainerItem container) ItemProperties.register(entry.get(),
                    ResourceLocation.fromNamespaceAndPath(Defines.MODID, "fill"),
                    (stack, level, entity, seed) -> container.contents(stack).getAmount() / (float)container.capacity());
        }));
    }
}
