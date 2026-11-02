package io.vinum.client;

import io.vinum.common.Defines;
import io.vinum.fluid.ModFluids;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = Defines.MODID, value = Dist.CLIENT)
public final class BeverageFluidClient {
    private BeverageFluidClient() {}
    @SubscribeEvent
    public static void register(RegisterClientExtensionsEvent event) {
        for (var entry : ModFluids.entries()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() { return ResourceLocation.withDefaultNamespace("block/water_still"); }
                @Override
                public ResourceLocation getFlowingTexture() { return ResourceLocation.withDefaultNamespace("block/water_flow"); }
                @Override
                public int getTintColor() { return entry.color(); }
            }, entry.type().get());
        }
    }
}
