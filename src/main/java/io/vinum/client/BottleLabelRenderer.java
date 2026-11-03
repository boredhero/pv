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
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(label.artwork()));
        // Follow the faceted bottle surface so alpha-shaped labels do not float as flat cards.
        int strips=shape.sides()==4?1:16;
        for(int strip=0;strip<strips;strip++) {
            double a=(double)strip/strips,b=(double)(strip+1)/strips;
            double x0=-half+2*half*a,x1=-half+2*half*b;
            double d0=depth(shape,radius,x0/scale,lying)*scale+.002;
            double d1=depth(shape,radius,x1/scale,lying)*scale+.002;
            double[][] vertices=lying
                ?new double[][]{{x0,d0,.88-panel.bottom()*1.4},{x0,d0,.88-panel.top()*1.4},{x1,d1,.88-panel.top()*1.4},{x1,d1,.88-panel.bottom()*1.4}}
                :new double[][]{{x0,panel.bottom(),-d0},{x0,panel.top(),-d0},{x1,panel.top(),-d1},{x1,panel.bottom(),-d1}};
            float u0=(float)(panel.u0()+(panel.u1()-panel.u0())*a),u1=(float)(panel.u0()+(panel.u1()-panel.u0())*b);
            for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2])
                .setColor(label.primaryColor()).setUv(i<2?u0:u1,i==0||i==3?panel.v1():panel.v0())
                .setOverlay(overlay).setLight(light).setNormal(pose,0,lying?1:0,lying?0:-1);
        }
    }
    private static double depth(BottleGeometry shape,double radius,double x,boolean lying) {
        double offset=shape.sides()==4?Math.PI/4:lying?Math.PI/8:0;
        double deepest=0;
        for(int i=0;i<shape.sides();i++) {
            double a=offset+2*Math.PI*i/shape.sides(),b=offset+2*Math.PI*(i+1)/shape.sides();
            double x0=Math.cos(a)*radius,x1=Math.cos(b)*radius;
            if(Math.abs(x1-x0)<1e-9||x<Math.min(x0,x1)-1e-9||x>Math.max(x0,x1)+1e-9)continue;
            double y=Math.sin(a)*radius+(Math.sin(b)-Math.sin(a))*radius*(x-x0)/(x1-x0);
            deepest=Math.max(deepest,lying?y:-y);
        }
        return deepest;
    }
}
