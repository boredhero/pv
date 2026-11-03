package io.vinum.shelf;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.vinum.beverage.*;
import io.vinum.item.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** True three-dimensional cellar bottles, with stored contents selecting shape and label palette. */
@EventBusSubscriber(modid="vinum", value=Dist.CLIENT)
public final class WineShelfRenderer implements BlockEntityRenderer<WineShelfBlockEntity> {
  private static final ResourceLocation MATERIAL=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
  public WineShelfRenderer(BlockEntityRendererProvider.Context context) {}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(ShelfContent.ENTITY.get(),WineShelfRenderer::new); }
  @Override public void render(WineShelfBlockEntity shelf,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
    poses.pushPose(); poses.translate(.5,.5,.5);
    poses.mulPose(Axis.YP.rotationDegrees(180-shelf.getBlockState().getValue(WineShelfBlock.FACING).toYRot()));
    poses.translate(-.5,-.5,-.5);
    for(int slot=0;slot<WineShelfBlockEntity.SLOTS;slot++) {
      var stack=shelf.bottle(slot); if(!(stack.getItem() instanceof FluidContainerItem container))continue;
      var fluid=container.contents(stack); var style=BeverageAppearance.resolve(fluid, stack);
      var shape=style.bottle3DType().geometry();
      double x=WineShelfBlock.CENTERS[slot][0],y=WineShelfBlock.CENTERS[slot][1];
      // Reuse the exact catalog profile, rotated neck-out and scaled to fit the rack.
      double radialScale=1.4, lengthScale=1.4;
      for(int i=0;i<shape.sections();i++) {
        double low=shape.height(i), high=shape.height(i+1);
        if(!fluid.isEmpty())
          tube(poses,buffers,x,y,.88-high*lengthScale,.88-low*lengthScale,
              shape.radius(high)*radialScale*.86,shape.radius(low)*radialScale*.86,
              WineCatalog.color(fluid),shape.sides(),light,overlay,MATERIAL,false);
        tube(poses,buffers,x,y,.88-high*lengthScale,.88-low*lengthScale,
            shape.radius(high)*radialScale,shape.radius(low)*radialScale,
            style.glassMaterial().color(),shape.sides(),light,overlay,
            style.glassMaterial().texture(),true);
      }
      if(style.closure().type()!=BeverageAppearance.ClosureType.NONE
          && !fluid.isEmpty() && (style.closure().type()!=BeverageAppearance.ClosureType.CORK || BottleCorks.hasCork(stack)))
        tube(poses,buffers,x,y,.09,.13,.04,.04,style.closure().color(),shape.sides(),light,overlay,style.closure().texture(),false);
      if(!fluid.isEmpty()) {
        tube(poses,buffers,x,y,.47,.66,shape.radius(.29)*radialScale+.001,shape.radius(.16)*radialScale+.001,
            style.label().primaryColor(),shape.sides(),light,overlay,style.label().artwork(),false);
        for(double near:new double[]{.475,.64}) {
          double far=near+.015;
          tube(poses,buffers,x,y,near,far,shape.radius((.88-near)/lengthScale)*radialScale+.002,
              shape.radius((.88-far)/lengthScale)*radialScale+.002,style.label().secondaryColor(),
              shape.sides(),light,overlay,style.label().artwork(),false);
        }
      }

    }
    poses.popPose();
  }
  private static void tube(PoseStack poses,MultiBufferSource buffers,double x,double y,double near,double far,double r1,double r2,int color,int sides,int light,int overlay,ResourceLocation texture,boolean translucent) {
    VertexConsumer out=buffers.getBuffer(translucent?RenderType.entityTranslucent(texture):RenderType.entityCutoutNoCull(texture));
    double offset=sides==4?Math.PI/4:Math.PI/8;
    for(int i=0;i<sides;i++) {
      double a=offset+i*Math.PI*2/sides,b=offset+(i+1)*Math.PI*2/sides;
      double c1=Math.cos(a),s1=Math.sin(a),c2=Math.cos(b),s2=Math.sin(b);
      quad(out,poses.last(),new double[][]{{x+c1*r1,y+s1*r1,near},{x+c2*r1,y+s2*r1,near},{x+c2*r2,y+s2*r2,far},{x+c1*r2,y+s1*r2,far}},color,(float)Math.cos((a+b)/2),(float)Math.sin((a+b)/2),0,light,overlay);
      quad(out,poses.last(),new double[][]{{x,y,near},{x,y,near},{x+c2*r1,y+s2*r1,near},{x+c1*r1,y+s1*r1,near}},color,0,0,-1,light,overlay);
      quad(out,poses.last(),new double[][]{{x,y,far},{x,y,far},{x+c1*r2,y+s1*r2,far},{x+c2*r2,y+s2*r2,far}},color,0,0,1,light,overlay);
    }
  }
  private static void quad(VertexConsumer out,PoseStack.Pose pose,double[][] vertices,int color,float nx,float ny,float nz,int light,int overlay) {
    for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2]).setColor(color).setUv(i==0||i==3?0:1,i<2?0:1).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
  }
}
