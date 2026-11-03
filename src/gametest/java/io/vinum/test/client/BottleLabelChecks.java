package io.vinum.test.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vinum.beverage.BeverageAppearance;
import io.vinum.beverage.BottleGeometry;
import io.vinum.client.BottleLabelRenderer;
import net.minecraft.resources.ResourceLocation;

/** Readable artwork must advance U toward the viewer's right, for bars and racks independently. */
final class BottleLabelChecks {
    static void run() {
        // Asymmetric atlas crop catches both a mirrored full image and an accidental 1-U crop shift.
        var panel = new BeverageAppearance.LabelPanel(.65f,.08f,.30f,.125f,.20f,.75f,.90f);
        var label = new BeverageAppearance.Label(ResourceLocation.fromNamespaceAndPath("vinum",
            "textures/block/neutral_spirit_label.png"), -1, -1, panel);
        for (var shape : BottleGeometry.values()) for (boolean lying : new boolean[]{false,true}) {
            var probe = new Probe(lying,panel);
            BottleLabelRenderer.draw(type -> probe,new PoseStack().last(),label,shape,1,lying,0,0);
            if (probe.count==0 || probe.count%4!=0) throw new AssertionError("Incomplete label geometry");
        }
    }

    private static final class Probe implements VertexConsumer {
        private final boolean lying;
        private final BeverageAppearance.LabelPanel panel;
        private float x,u,v,firstX,firstU,firstV;
        private int count;
        Probe(boolean lying,BeverageAppearance.LabelPanel panel){this.lying=lying;this.panel=panel;}
        @Override public VertexConsumer addVertex(float x,float y,float z){this.x=x;return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        @Override public VertexConsumer setUv(float u,float v){this.u=u;this.v=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){return this;}
        @Override public VertexConsumer setNormal(float nx,float ny,float nz){
            if (u<panel.u0()-1e-6 || u>panel.u1()+1e-6 || v<panel.v0()-1e-6 || v>panel.v1()+1e-6)
                throw new AssertionError("Artwork escaped its cropped atlas rectangle");
            int corner=count++%4;
            if(corner==0){firstX=x;firstU=u;firstV=v;}
            if(corner==1 && v>=firstV)throw new AssertionError("Label image is upside down");
            if(corner==3){
                float screenRightDelta=(lying?1:-1)*(x-firstX);
                if(screenRightDelta*(u-firstU)<=0)
                    throw new AssertionError("Label is mirrored from its outward-facing side (lying="+lying+")");
            }
            return this;
        }
    }
}
