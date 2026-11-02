package io.vinum.machine;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import io.vinum.tileentity.ModTileEntities;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Moving platen and installed crank; geometry is independent of fluid/processing state. */
@EventBusSubscriber(modid = "vinum", value = Dist.CLIENT)
public final class WinePressRenderer implements BlockEntityRenderer<WinePressBlockEntity> {
  private static final ResourceLocation WOOD =
      ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");
  private static final ResourceLocation IRON =
      ResourceLocation.withDefaultNamespace("textures/block/purple_concrete.png");

  public WinePressRenderer(BlockEntityRendererProvider.Context context) {}

  @SubscribeEvent
  public static void register(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(ModTileEntities.WINE_PRESS.get(), WinePressRenderer::new);
  }

  @Override
  public void render(
      WinePressBlockEntity press,
      float partial,
      PoseStack poses,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    float elapsed =
        press.getLevel() == null
            ? 1
            : Math.clamp(
                (press.getLevel().getGameTime() + partial - press.lastCrank())
                    / WinePressBlockEntity.CRANK_COOLDOWN,
                0,
                1);
    float fraction = press.progressFraction();
    if (press.crankTurns() > 0 && press.requiredWork() > 0)
      fraction =
          Math.max(
              0, fraction - (1 - elapsed) * WinePressBlockEntity.CRANK_WORK / press.requiredWork());
    float down = fraction * .22F;
    var wood = buffers.getBuffer(RenderType.entityCutoutNoCull(WOOD));
    // Thin, split octagonal pressing plates fit inside the stave basket.
    for (int side = 0; side < 2; side++) {
      float left = side == 0 ? .225F : .505F, right = side == 0 ? .495F : .775F;
      box(wood, poses, left, 1.10F - down, .32F, right, 1.15F - down, .68F, light, overlay);
      box(
          wood,
          poses,
          side == 0 ? .30F : .505F,
          1.10F - down,
          .235F,
          side == 0 ? .495F : .70F,
          1.15F - down,
          .32F,
          light,
          overlay);
      box(
          wood,
          poses,
          side == 0 ? .30F : .505F,
          1.10F - down,
          .68F,
          side == 0 ? .495F : .70F,
          1.15F - down,
          .765F,
          light,
          overlay);
    }
    var metal = buffers.getBuffer(RenderType.entityCutoutNoCull(IRON));
    box(metal, poses, .43F, 1.15F - down, .43F, .57F, 1.25F - down, .57F, light, overlay);
    if (press.hasMotor()) {
      box(metal, poses, .33F, 1.26F - down, .35F, .67F, 1.43F - down, .65F, light, overlay);
    }
    if (!press.hasCrank()) return;
    float angle = press.crankTurns() == 0 ? 0 : (press.crankTurns() - 1 + elapsed) * 90F;
    poses.pushPose();
    poses.translate(.5, 1.23 - down, .5);
    poses.mulPose(Axis.YP.rotationDegrees(angle));
    // One slim radial lever with a small grip, attached at the ratchet nut.
    box(metal, poses, 0, 0, -.022F, .39F, .035F, .022F, light, overlay);
    // Switching render types may finish the previous buffer; reacquire it before the grip.
    box(
        buffers.getBuffer(RenderType.entityCutoutNoCull(WOOD)),
        poses,
        .33F,
        -.018F,
        -.038F,
        .44F,
        .053F,
        .038F,
        light,
        overlay);
    poses.popPose();
  }

  @Override
  public net.minecraft.world.phys.AABB getRenderBoundingBox(WinePressBlockEntity press) {
    var pos = press.getBlockPos();
    return new net.minecraft.world.phys.AABB(
        pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
  }

  private static void box(
      VertexConsumer out,
      PoseStack poses,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int light,
      int overlay) {
    var p = poses.last();
    quad(
        out,
        p,
        new float[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}},
        0,
        0,
        -1,
        light,
        overlay);
    quad(
        out,
        p,
        new float[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}},
        0,
        0,
        1,
        light,
        overlay);
    quad(
        out,
        p,
        new float[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}},
        -1,
        0,
        0,
        light,
        overlay);
    quad(
        out,
        p,
        new float[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}},
        1,
        0,
        0,
        light,
        overlay);
    quad(
        out,
        p,
        new float[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}},
        0,
        1,
        0,
        light,
        overlay);
    quad(
        out,
        p,
        new float[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}},
        0,
        -1,
        0,
        light,
        overlay);
  }

  private static void quad(
      VertexConsumer out,
      PoseStack.Pose pose,
      float[][] points,
      int nx,
      int ny,
      int nz,
      int light,
      int overlay) {
    for (int i = 0; i < 4; i++)
      out.addVertex(pose, points[i][0], points[i][1], points[i][2])
          .setColor(255, 255, 255, 255)
          .setUv(i == 1 || i == 2 ? 1 : 0, i >= 2 ? 0 : 1)
          .setOverlay(overlay)
          .setLight(light)
          .setNormal(pose, nx, ny, nz);
  }
}
