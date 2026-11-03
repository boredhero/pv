package io.vinum.bar;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.vinum.beverage.*;
import io.vinum.item.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Separate glass, labels and continuously filled liquid meshes, using the item appearance catalog. */
@EventBusSubscriber(modid="vinum", value=Dist.CLIENT)
public final class BarRenderer implements BlockEntityRenderer<BarBlockEntity> {
    private static final ResourceLocation MATERIAL=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public BarRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BarContent.ENTITY.get(),BarRenderer::new);
    }
    @Override public AABB getRenderBoundingBox(BarBlockEntity bar) { return new AABB(bar.getBlockPos()).expandTowards(0,.65,0); }
    @Override public void render(BarBlockEntity bar,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
        poses.pushPose(); poses.translate(.5,0,.5);
        poses.mulPose(Axis.YP.rotationDegrees(180-bar.getBlockState().getValue(BarBlock.FACING).toYRot()));
        poses.translate(-.5,0,-.5);
        var meshes=io.vinum.client.SlotRenderCache.get(bar,bar.renderRevision(),BarBlockEntity.SLOTS,
            slot->bottleMesh(bar.bottle(slot)));
        for(int slot=0;slot<meshes.length;slot++) {
            if(meshes[slot]==null)continue;
            poses.pushPose();
            poses.translate(BarBlock.CENTERS[slot][0],1.008,BarBlock.CENTERS[slot][1]);
            meshes[slot].render(poses.last(),buffers,light,overlay);
            poses.popPose();
        }
        poses.popPose();
    }
    private record BottleKey(BeverageAppearance.Style style,int color,double fraction,boolean filled,boolean closed) {}
    private static final io.vinum.client.RenderMeshCache<BottleKey> MESHES=new io.vinum.client.RenderMeshCache<>(256);
    private static io.vinum.client.CachedRenderMesh bottleMesh(net.minecraft.world.item.ItemStack stack) {
        var drinkware=DrinkwareAppearance.find(stack);
        if(drinkware.isPresent())return DrinkwareRenderer.mesh(stack,drinkware.get());
        if(!(stack.getItem() instanceof FluidContainerItem container))return null;
        var fluid=container.contents(stack);
        var style=BeverageAppearance.resolve(fluid,stack);
        var key=new BottleKey(style,0xFF000000|WineCatalog.color(fluid),
            fluid.getAmount()/(double)container.capacity(),!fluid.isEmpty(),BottleCorks.hasCork(stack));
        return MESHES.get(key,()->io.vinum.client.CachedRenderMesh.record((poses,buffers)->drawBottle(key,poses,buffers)));
    }
    /** Build only on a changed appearance/fill; replay retains the exact tessellation and materials. */
    private static void drawBottle(BottleKey key,PoseStack poses,MultiBufferSource buffers) {
        var style=key.style();var shape=style.bottle3DType().geometry();
        int light=0,overlay=0;
        poses.scale((float)shape.widthScale(),1,(float)shape.depthScale());
        var opaque=buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
        if(key.filled()) {
            double surface=shape.fillHeight(key.fraction());
            for(int i=0;i<shape.sections();i++) {
                double low=Math.max(shape.height(i),shape.liquidBottom()),high=Math.min(shape.height(i+1),surface);
                if(high<=low)continue;
                wall(opaque,poses.last(),shape,low+.008,high+.008,shape.radius(low)*.86,shape.radius(high)*.86,key.color(),light,overlay);
            }
            cap(opaque,poses.last(),shape,surface+.008,shape.radius(surface)*.86,key.color(),light,overlay);
        }
        int edge=0xFF000000|style.glassMaterial().tint();
        wall(opaque,poses.last(),shape,0,.012,shape.radius(0),shape.radius(.012),edge,light,overlay);
        double lip=shape.height(shape.sections());
        wall(opaque,poses.last(),shape,lip-.008,lip,shape.radius(lip-.008),shape.radius(lip),edge,light,overlay);
        if(key.filled())io.vinum.client.BottleLabelRenderer.draw(buffers,poses.last(),style.label(),shape,1,false,light,overlay);
        if(key.filled()&&style.closure().type()!=BeverageAppearance.ClosureType.NONE
            &&(style.closure().type()!=BeverageAppearance.ClosureType.CORK||key.closed()))
            io.vinum.client.BottleClosureRenderer.draw(buffers,poses.last(),style.closure(),shape,light,overlay);
        var glass=buffers.getBuffer(RenderType.entityTranslucent(style.glassMaterial().texture()));
        for(int i=0;i<shape.sections();i++)wall(glass,poses.last(),shape,shape.height(i),shape.height(i+1),
            shape.radius(shape.height(i)),shape.radius(shape.height(i+1)),style.glassMaterial().color(),light,overlay);
    }
    static void wall(VertexConsumer out,PoseStack.Pose pose,BottleGeometry shape,double low,double high,double r0,double r1,int color,int light,int overlay) {
        int sides=shape.sides();double offset=sides==4?Math.PI/4:0;
        for(int i=0;i<sides;i++) {
            double a=offset+2*Math.PI*i/sides,b=offset+2*Math.PI*(i+1)/sides,mid=(a+b)/2;
            double slope=(r0-r1)/(high-low),len=Math.sqrt(1+slope*slope);
            quad(out,pose,new double[][]{{Math.cos(a)*r0,low,Math.sin(a)*r0},{Math.cos(a)*r1,high,Math.sin(a)*r1},{Math.cos(b)*r1,high,Math.sin(b)*r1},{Math.cos(b)*r0,low,Math.sin(b)*r0}},color,(float)(Math.cos(mid)/len),(float)(slope/len),(float)(Math.sin(mid)/len),light,overlay);
        }
    }
    static void cap(VertexConsumer out,PoseStack.Pose pose,BottleGeometry shape,double y,double radius,int color,int light,int overlay) {
        double offset=shape.sides()==4?Math.PI/4:0;
        for(int i=0;i<shape.sides();i++) {
            double a=offset+2*Math.PI*i/shape.sides(),b=offset+2*Math.PI*(i+1)/shape.sides();
            quad(out,pose,new double[][]{{0,y,0},{0,y,0},{Math.cos(b)*radius,y,Math.sin(b)*radius},{Math.cos(a)*radius,y,Math.sin(a)*radius}},color,0,1,0,light,overlay);
        }
    }
    static void quad(VertexConsumer out,PoseStack.Pose pose,double[][] vertices,int color,float nx,float ny,float nz,int light,int overlay) {
        for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2]).setColor(color).setUv(i==0||i==3?0:1,i<2?0:1).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
