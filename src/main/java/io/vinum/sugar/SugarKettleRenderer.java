package io.vinum.sugar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** The copper model supplies the walls; the synchronized tank supplies its visible liquid surface. */
@EventBusSubscriber(modid="vinum", value=Dist.CLIENT)
public final class SugarKettleRenderer implements BlockEntityRenderer<SugarKettleBlockEntity> {
    public SugarKettleRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SugarContent.ENTITY.get(),SugarKettleRenderer::new);
    }
    @Override public void render(SugarKettleBlockEntity kettle,float partialTick,PoseStack poses,
            MultiBufferSource buffers,int light,int overlay) {
        var fluid=kettle.contents();
        if(fluid.isEmpty())return;
        var appearance=IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture=appearance.getStillTexture(fluid);
        if(texture==null)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        // Interior spans 3..13 pixels, above the floor at4 and below the rim at13.
        float low=3.01F/16,high=12.99F/16;
        float fill=Math.clamp(fluid.getAmount()/(float)SugarKettleBlockEntity.CAPACITY,0,1);
        float y=(4.02F+8.48F*fill)/16;
        int color=appearance.getTintColor(fluid);
        var out=buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        vertex(out,poses.last(),low,y,low,sprite.getU(low),sprite.getV(low),color,light,overlay);
        vertex(out,poses.last(),low,y,high,sprite.getU(low),sprite.getV(high),color,light,overlay);
        vertex(out,poses.last(),high,y,high,sprite.getU(high),sprite.getV(high),color,light,overlay);
        vertex(out,poses.last(),high,y,low,sprite.getU(high),sprite.getV(low),color,light,overlay);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float z,
            float u,float v,int color,int light,int overlay) {
        out.addVertex(pose,x,y,z).setColor(color).setUv(u,v).setOverlay(overlay)
            .setLight(light).setNormal(pose,0,1,0);
    }
}
