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
        for(int slot=0;slot<BarBlockEntity.SLOTS;slot++) {
            var stack=bar.bottle(slot);
            if(!(stack.getItem() instanceof FluidContainerItem container))continue;
            var fluid=container.contents(stack);var style=BeverageAppearance.resolve(fluid, stack);
            var shape=style.bottle3DType().geometry();
            int primary=style.label().primaryColor();
            int secondary=style.label().secondaryColor();
            poses.pushPose();poses.translate(BarBlock.CENTERS[slot][0],1.008,BarBlock.CENTERS[slot][1]);
            poses.scale((float)shape.widthScale(),1,(float)shape.depthScale());
            var opaque=buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
            if(!fluid.isEmpty()) {
                double surface=shape.fillHeight((double)fluid.getAmount()/container.capacity());
                for(int i=0;i<shape.sections();i++) {
                    double low=shape.height(i),high=Math.min(shape.height(i+1),surface);
                    if(high<=low)break;
                    wall(opaque,poses.last(),shape,low+.008,high+.008,shape.radius(low)*.86,shape.radius(high)*.86,0xFF000000|WineCatalog.color(fluid),light,overlay,false);
                }
                cap(opaque,poses.last(),shape,surface+.008,shape.radius(surface)*.86,0xFF000000|WineCatalog.color(fluid),light,overlay);
                var label=buffers.getBuffer(RenderType.entityCutoutNoCull(style.label().artwork()));
                // Labels face the customer, leaving the sides and back clear to see liquid height.
                wall(label,poses.last(),shape,.14,.285,shape.radius(.14)+.001,shape.radius(.285)+.001,primary,light,overlay,true);
                wall(label,poses.last(),shape,.14,.157,shape.radius(.14)+.002,shape.radius(.157)+.002,secondary,light,overlay,true);
                wall(label,poses.last(),shape,.268,.285,shape.radius(.268)+.002,shape.radius(.285)+.002,secondary,light,overlay,true);
                wall(label,poses.last(),shape,.195,.225,shape.radius(.195)+.002,shape.radius(.225)+.002,secondary,light,overlay,true);
            }
            // Thin opaque heel/lip highlights keep the glass silhouette legible when empty.
            int edge=0xFF000000 | style.glassMaterial().tint();
            wall(opaque,poses.last(),shape,0,.012,shape.radius(0),shape.radius(.012),edge,light,overlay,false);
            wall(opaque,poses.last(),shape,.526,.54,.029,.029,edge,light,overlay,false);
            if(!fluid.isEmpty() && style.closure().type()!=BeverageAppearance.ClosureType.NONE
                && (style.closure().type()!=BeverageAppearance.ClosureType.CORK || BottleCorks.hasCork(stack))) {
                int closure=style.closure().color();
                var closureMesh=buffers.getBuffer(RenderType.entityCutoutNoCull(style.closure().texture()));
                wall(closureMesh,poses.last(),shape,.535,.563,.028,.028,closure,light,overlay,false);
                cap(closureMesh,poses.last(),shape,.563,.028,closure,light,overlay);
            }
            var glass=buffers.getBuffer(RenderType.entityTranslucent(style.glassMaterial().texture()));
            int glassColor=style.glassMaterial().color();
            for(int i=0;i<shape.sections();i++)wall(glass,poses.last(),shape,shape.height(i),shape.height(i+1),shape.radius(shape.height(i)),shape.radius(shape.height(i+1)),glassColor,light,overlay,false);
            poses.popPose();
        }
        poses.popPose();
    }
    private static void wall(VertexConsumer out,PoseStack.Pose pose,BottleGeometry shape,double low,double high,double r0,double r1,int color,int light,int overlay,boolean label) {
        int sides=shape.sides();double offset=sides==4?Math.PI/4:0;
        for(int i=0;i<sides;i++) {
            double a=offset+2*Math.PI*i/sides,b=offset+2*Math.PI*(i+1)/sides,mid=(a+b)/2;
            if(label && Math.sin(mid)>-.65)continue;
            double slope=(r0-r1)/(high-low),len=Math.sqrt(1+slope*slope);
            quad(out,pose,new double[][]{{Math.cos(a)*r0,low,Math.sin(a)*r0},{Math.cos(a)*r1,high,Math.sin(a)*r1},{Math.cos(b)*r1,high,Math.sin(b)*r1},{Math.cos(b)*r0,low,Math.sin(b)*r0}},color,(float)(Math.cos(mid)/len),(float)(slope/len),(float)(Math.sin(mid)/len),light,overlay);
        }
    }
    private static void cap(VertexConsumer out,PoseStack.Pose pose,BottleGeometry shape,double y,double radius,int color,int light,int overlay) {
        double offset=shape.sides()==4?Math.PI/4:0;
        for(int i=0;i<shape.sides();i++) {
            double a=offset+2*Math.PI*i/shape.sides(),b=offset+2*Math.PI*(i+1)/shape.sides();
            quad(out,pose,new double[][]{{0,y,0},{0,y,0},{Math.cos(b)*radius,y,Math.sin(b)*radius},{Math.cos(a)*radius,y,Math.sin(a)*radius}},color,0,1,0,light,overlay);
        }
    }
    private static void quad(VertexConsumer out,PoseStack.Pose pose,double[][] vertices,int color,float nx,float ny,float nz,int light,int overlay) {
        for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2]).setColor(color).setUv(i==0||i==3?0:1,i<2?0:1).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
