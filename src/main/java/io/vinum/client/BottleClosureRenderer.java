package io.vinum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vinum.beverage.BeverageAppearance;
import io.vinum.beverage.BottleGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** One continuous cylindrical wrap and planar end mapping, shared by upright and racked bottles. */
public final class BottleClosureRenderer {
    private BottleClosureRenderer() {}

    public static void draw(MultiBufferSource buffers, PoseStack.Pose pose,
                            BeverageAppearance.Closure closure, BottleGeometry shape, int light, int overlay) {
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(closure.texture()));
        int sides = shape.sides();
        double offset = sides == 4 ? Math.PI / 4 : 0;
        for (int i = 0; i < sides; i++) {
            double a = offset + 2 * Math.PI * i / sides, b = offset + 2 * Math.PI * (i + 1) / sides;
            double ax = Math.cos(a), az = Math.sin(a), bx = Math.cos(b), bz = Math.sin(b);
            float u0 = (float)i / sides, u1 = (float)(i + 1) / sides;
            float nx = (float)Math.cos((a + b) / 2), nz = (float)Math.sin((a + b) / 2);
            double r = closure.radius();
            vertex(out, pose, ax*r, closure.bottom(), az*r, u0, 1, closure.color(), nx, 0, nz, light, overlay);
            vertex(out, pose, ax*r, closure.top(), az*r, u0, 0, closure.color(), nx, 0, nz, light, overlay);
            vertex(out, pose, bx*r, closure.top(), bz*r, u1, 0, closure.color(), nx, 0, nz, light, overlay);
            vertex(out, pose, bx*r, closure.bottom(), bz*r, u1, 1, closure.color(), nx, 0, nz, light, overlay);
            // Degenerate quads form a triangle fan. Both center vertices must share the center UV.
            vertex(out, pose, 0, closure.top(), 0, .5f, .5f, closure.color(), 0, 1, 0, light, overlay);
            vertex(out, pose, 0, closure.top(), 0, .5f, .5f, closure.color(), 0, 1, 0, light, overlay);
            vertex(out, pose, bx*r, closure.top(), bz*r, (float)(.5+.5*bx), (float)(.5+.5*bz), closure.color(), 0, 1, 0, light, overlay);
            vertex(out, pose, ax*r, closure.top(), az*r, (float)(.5+.5*ax), (float)(.5+.5*az), closure.color(), 0, 1, 0, light, overlay);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, double x, double y, double z,
                               float u, float v, int color, float nx, float ny, float nz, int light, int overlay) {
        out.addVertex(pose, (float)x, (float)y, (float)z).setColor(color).setUv(u,v)
            .setOverlay(overlay).setLight(light).setNormal(pose,nx,ny,nz);
    }
}
