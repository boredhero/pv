package io.vinum.test.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vinum.beverage.BeverageAppearance;
import io.vinum.beverage.BottleGeometry;
import io.vinum.client.BottleClosureRenderer;
import net.minecraft.resources.ResourceLocation;

/** Regression for mirrored cork wedges: coincident cap positions must have identical planar UVs. */
final class BottleClosureChecks {
    static void run() {
        var shape = BottleGeometry.byId(ResourceLocation.fromNamespaceAndPath("vinum", "wine"));
        var closure = new BeverageAppearance.Closure(BeverageAppearance.ClosureType.CORK,
            ResourceLocation.fromNamespaceAndPath("vinum", "textures/block/bottle_cork.png"), -1, .5, .56, .023);
        var consumer = new Probe(closure.radius());
        BottleClosureRenderer.draw(type -> consumer, new PoseStack().last(), closure, shape, 0, 0);
        if (consumer.capVertices != shape.sides()*4 || consumer.sideVertices != shape.sides()*4)
            throw new AssertionError("Closure mesh did not exercise both sides and cap");
    }

    private static final class Probe implements VertexConsumer {
        private final double radius;
        private float x,z,u,v;
        int capVertices,sideVertices;
        Probe(double radius) { this.radius=radius; }
        @Override public VertexConsumer addVertex(float x,float y,float z) { this.x=x;this.z=z;return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { return this; }
        @Override public VertexConsumer setUv(float u,float v) { this.u=u;this.v=v;return this; }
        @Override public VertexConsumer setUv1(int u,int v) { return this; }
        @Override public VertexConsumer setUv2(int u,int v) { return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) {
            if(y>.99f) {
                capVertices++;
                if(Math.abs(u-(.5+.5*this.x/radius))>1e-5 || Math.abs(v-(.5+.5*this.z/radius))>1e-5)
                    throw new AssertionError("Cork cap repeats or mirrors UVs across a triangle seam");
            } else {
                sideVertices++;
                if(u<0 || u>1 || v<0 || v>1)throw new AssertionError("Closure wrap UV outside texture");
            }
            return this;
        }
    }
}
