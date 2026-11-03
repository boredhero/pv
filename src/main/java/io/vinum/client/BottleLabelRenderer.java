package io.vinum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.vinum.beverage.*;
import net.minecraft.client.renderer.*;

/** A centered artwork patch, never a decorative band around the whole bottle. */
public final class BottleLabelRenderer {
    private BottleLabelRenderer() {}
    public static void draw(MultiBufferSource buffers,PoseStack.Pose pose,BeverageAppearance.Label label,
                            BottleGeometry shape,double scale,boolean lying,int light,int overlay) {
        var panel=label.panel();
        double radius=Math.max(shape.radius(panel.bottom()),shape.radius(panel.top()));
        double face=shape.sides()==4?Math.sqrt(.5):1;
        double half=radius*face*panel.width()*scale;
        double depth=(radius*face+.002)*scale;
        double[][] vertices=lying
            ?new double[][]{{-half,depth,.88-panel.bottom()*1.4},{-half,depth,.88-panel.top()*1.4},{half,depth,.88-panel.top()*1.4},{half,depth,.88-panel.bottom()*1.4}}
            :new double[][]{{-half,panel.bottom(),-depth},{-half,panel.top(),-depth},{half,panel.top(),-depth},{half,panel.bottom(),-depth}};
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(label.artwork()));
        for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2])
            .setColor(label.primaryColor()).setUv(i<2?panel.u0():panel.u1(),i==0||i==3?panel.v1():panel.v0())
            .setOverlay(overlay).setLight(light).setNormal(pose,0,lying?1:0,lying?0:-1);
    }
}
