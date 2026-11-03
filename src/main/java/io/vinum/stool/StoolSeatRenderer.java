package io.vinum.stool;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="vinum", value=Dist.CLIENT)
public final class StoolSeatRenderer extends EntityRenderer<StoolSeat> {
    public StoolSeatRenderer(EntityRendererProvider.Context context) { super(context); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(StoolContent.SEAT.get(),StoolSeatRenderer::new);
    }
    @Override public void render(StoolSeat seat,float yaw,float partialTick,PoseStack poses,MultiBufferSource buffers,int light) {}
    @Override public ResourceLocation getTextureLocation(StoolSeat seat) { return ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png"); }
}
