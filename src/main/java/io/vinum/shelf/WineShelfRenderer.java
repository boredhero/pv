package io.vinum.shelf;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.vinum.beverage.*;
import io.vinum.item.*;
import io.vinum.client.CachedRenderMesh;
import io.vinum.client.RenderMeshCache;
import io.vinum.client.SlotRenderCache;
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
  private record MeshKey(BeverageAppearance.Style style, int liquidColor, boolean filled, boolean closed) {}
  private static final RenderMeshCache<MeshKey> MESHES = new RenderMeshCache<>(256);
  public WineShelfRenderer(BlockEntityRendererProvider.Context context) {}
  @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) { event.registerBlockEntityRenderer(ShelfContent.ENTITY.get(),WineShelfRenderer::new); }
  @Override public void render(WineShelfBlockEntity shelf,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay) {
    poses.pushPose(); poses.translate(.5,.5,.5);
    poses.mulPose(Axis.YP.rotationDegrees(180-shelf.getBlockState().getValue(WineShelfBlock.FACING).toYRot()));
    poses.translate(-.5,-.5,-.5);
    var meshes = SlotRenderCache.get(shelf, shelf.renderRevision(), WineShelfBlockEntity.SLOTS,
        slot -> bottleMesh(shelf.bottle(slot)));
    for(int slot=0;slot<WineShelfBlockEntity.SLOTS;slot++) {
      var mesh = meshes[slot];
      if (mesh == null) continue;
      poses.pushPose();
      poses.translate(WineShelfBlock.CENTERS[slot][0],WineShelfBlock.CENTERS[slot][1],0);
      mesh.render(poses.last(), buffers, light, overlay);
      poses.popPose();
    }
    poses.popPose();
  }
  private static CachedRenderMesh bottleMesh(net.minecraft.world.item.ItemStack stack) {
    if (!(stack.getItem() instanceof FluidContainerItem container)) return null;
    var fluid=container.contents(stack);
    var style=BeverageAppearance.resolve(fluid, stack);
    boolean filled = !fluid.isEmpty();
    boolean closed = filled && style.closure().type()!=BeverageAppearance.ClosureType.NONE
        && (style.closure().type()!=BeverageAppearance.ClosureType.CORK || BottleCorks.hasCork(stack));
    // Rack meshes intentionally preserve their existing full-bottle presentation. Inventory state
    // is resolved after updates, while expensive geometry is rebuilt only for a new appearance key.
    var key = new MeshKey(style, filled ? WineCatalog.color(fluid) : 0, filled, closed);
    return MESHES.get(key, () -> CachedRenderMesh.record((localPoses, localBuffers) ->
        drawBottle(key, localPoses, localBuffers, 0, 0)));
  }
  private static void drawBottle(MeshKey key, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
    var style = key.style();
    var shape = style.bottle3DType().geometry();
    poses.pushPose();
    poses.scale((float)shape.widthScale(),(float)shape.depthScale(),1);
    double radialScale=1.4/Math.max(shape.widthScale(),shape.depthScale()), lengthScale=1.4;
    // One opaque run instead of alternating material every profile section. Adjacent liquid
    // segments share an interior disk, which cannot be seen through the opaque liquid shell.
    if (key.filled()) for(int i=0;i<shape.sections();i++) {
      double low=shape.height(i), high=shape.height(i+1);
      tube(poses,buffers,0,0,.88-high*lengthScale,.88-low*lengthScale,
          shape.radius(high)*radialScale*.86,shape.radius(low)*radialScale*.86,
          key.liquidColor(),shape.sides(),light,overlay,MATERIAL,false,
          i==shape.sections()-1,i==0);
    }
    // Preserve every existing glass face and its ordering: transparent internal disks can
    // contribute visibly, so removing them is a separate visual change, not a safe cull.
    for(int i=0;i<shape.sections();i++) {
      double low=shape.height(i), high=shape.height(i+1);
      tube(poses,buffers,0,0,.88-high*lengthScale,.88-low*lengthScale,
          shape.radius(high)*radialScale,shape.radius(low)*radialScale,
          style.glassMaterial().color(),shape.sides(),light,overlay,
          style.glassMaterial().texture(),true,true,true);
    }
    if(key.closed()) {
      poses.pushPose();
      poses.translate(0,0,.88);
      poses.mulPose(Axis.XP.rotationDegrees(-90));
      poses.scale((float)radialScale,(float)lengthScale,(float)radialScale);
      io.vinum.client.BottleClosureRenderer.draw(buffers,poses.last(),style.closure(),shape,light,overlay);
      poses.popPose();
    }
    if(key.filled()) io.vinum.client.BottleLabelRenderer.draw(buffers,poses.last(),style.label(),shape,radialScale,true,light,overlay);
    poses.popPose();
  }
  private static void tube(PoseStack poses,MultiBufferSource buffers,double x,double y,double near,double far,double r1,double r2,int color,int sides,int light,int overlay,ResourceLocation texture,boolean translucent,boolean nearCap,boolean farCap) {
    VertexConsumer out=buffers.getBuffer(translucent?RenderType.entityTranslucent(texture):RenderType.entityCutoutNoCull(texture));
    double offset=sides==4?Math.PI/4:Math.PI/8;
    for(int i=0;i<sides;i++) {
      double a=offset+i*Math.PI*2/sides,b=offset+(i+1)*Math.PI*2/sides;
      double c1=Math.cos(a),s1=Math.sin(a),c2=Math.cos(b),s2=Math.sin(b);
      quad(out,poses.last(),new double[][]{{x+c1*r1,y+s1*r1,near},{x+c2*r1,y+s2*r1,near},{x+c2*r2,y+s2*r2,far},{x+c1*r2,y+s1*r2,far}},color,(float)Math.cos((a+b)/2),(float)Math.sin((a+b)/2),0,light,overlay);
      if(nearCap) quad(out,poses.last(),new double[][]{{x,y,near},{x,y,near},{x+c2*r1,y+s2*r1,near},{x+c1*r1,y+s1*r1,near}},color,0,0,-1,light,overlay);
      if(farCap) quad(out,poses.last(),new double[][]{{x,y,far},{x,y,far},{x+c1*r2,y+s1*r2,far},{x+c2*r2,y+s2*r2,far}},color,0,0,1,light,overlay);
    }
  }
  private static void quad(VertexConsumer out,PoseStack.Pose pose,double[][] vertices,int color,float nx,float ny,float nz,int light,int overlay) {
    for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2]).setColor(color).setUv(i==0||i==3?0:1,i<2?0:1).setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
  }
}
