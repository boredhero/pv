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
      var fluid=container.contents(stack); var style=BeverageAppearance.find(fluid);
      boolean wineBottle=stack.is(ModItems.WINE_BOTTLE_EMPTY.get());
      boolean tequila=style.map(s->s.bottleBaseType().equals("tequila")).orElse(false);
      int glass=wineBottle?0xFF314D28:0xFFABC3BC;
      int body=fluid.isEmpty()?glass:mix(glass,WineCatalog.color(fluid),wineBottle?.7:.85);
      int primary=style.map(BeverageAppearance.Style::bottleLabelPrimaryColor).orElse(0xFFE2D5B4);
      int secondary=style.map(BeverageAppearance.Style::bottleLabelSecondaryColor).orElse(0xFF705C36);
      double x=WineShelfBlock.CENTERS[slot][0],y=WineShelfBlock.CENTERS[slot][1];
      // Bottles lie neck-out; nine separate solid meshes occupy the rack's three-by-three openings.
      int sides=wineBottle?8:4;
      double radius=tequila?.125:.12;
      double heel=tequila?.098:radius;
      tube(poses,buffers,x,y,.34,.88,radius,heel,body,sides,light,overlay);
      tube(poses,buffers,x,y,.23,.34,.045,radius,glass,sides,light,overlay);
      tube(poses,buffers,x,y,.10,.23,.042,.045,glass,8,light,overlay);
      tube(poses,buffers,x,y,.065,.115,.044,.044,0xFFB18A55,8,light,overlay);
      if(!fluid.isEmpty()) {
        double taper=tequila?1:0;
        tube(poses,buffers,x,y,.47,.66,radius+.001-.006*taper,radius+.001-.016*taper,primary,sides,light,overlay);
        tube(poses,buffers,x,y,.475,.49,radius+.002-.005*taper,radius+.002-.006*taper,secondary,sides,light,overlay);
        tube(poses,buffers,x,y,.64,.655,radius+.002-.014*taper,radius+.002-.015*taper,secondary,sides,light,overlay);
      }
    }
    poses.popPose();
  }
  private static int mix(int a,int b,double ratio){int result=0xFF000000;for(int shift:new int[]{16,8,0})result|=(int)(((a>>shift)&255)*(1-ratio)+((b>>shift)&255)*ratio)<<shift;return result;}
  private static void tube(PoseStack poses,MultiBufferSource buffers,double x,double y,double near,double far,double r1,double r2,int color,int sides,int light,int overlay) {
    VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
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
