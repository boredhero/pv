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
        double face=shape.sides()==4?Math.sqrt(.5):1;
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(label.artwork()));
        // Split at every profile bend: each row hugs the local radius, including tapered decanters.
        var rows=new java.util.ArrayList<Double>();
        rows.add((double)panel.bottom());
        for(int section=0;section<=shape.sections();section++) {
            double height=shape.height(section);
            if(height>panel.bottom() && height<panel.top())rows.add(height);
        }
        rows.add((double)panel.top());
        int strips=shape.sides()==4?1:16;
        for(int row=0;row<rows.size()-1;row++) {
            double low=rows.get(row),high=rows.get(row+1);
            double lowerRadius=shape.radius(low),upperRadius=shape.radius(high);
            double lowerHalf=lowerRadius*face*panel.width()*scale;
            double upperHalf=upperRadius*face*panel.width()*scale;
            float lowerV=(float)(panel.v1()-(panel.v1()-panel.v0())*(low-panel.bottom())/(panel.top()-panel.bottom()));
            float upperV=(float)(panel.v1()-(panel.v1()-panel.v0())*(high-panel.bottom())/(panel.top()-panel.bottom()));
            for(int strip=0;strip<strips;strip++) {
                double a=(double)strip/strips,b=(double)(strip+1)/strips;
                double x0=-lowerHalf+2*lowerHalf*a,x1=-lowerHalf+2*lowerHalf*b;
                double x2=-upperHalf+2*upperHalf*a,x3=-upperHalf+2*upperHalf*b;
                double d0=depth(shape,lowerRadius,x0/scale,lying)*scale+.002;
                double d1=depth(shape,lowerRadius,x1/scale,lying)*scale+.002;
                double d2=depth(shape,upperRadius,x2/scale,lying)*scale+.002;
                double d3=depth(shape,upperRadius,x3/scale,lying)*scale+.002;
                double[][] vertices=lying
                    ?new double[][]{{x0,d0,.88-low*1.4},{x2,d2,.88-high*1.4},{x3,d3,.88-high*1.4},{x1,d1,.88-low*1.4}}
                    :new double[][]{{x0,low,-d0},{x2,high,-d2},{x3,high,-d3},{x1,low,-d1}};
                // Front (-Z) is viewed with screen-right toward -X. Rack labels (+Y),
                // with the neck toward -Z, have screen-right toward +X instead.
                double leftU=lying?a:1-a,rightU=lying?b:1-b;
                float u0=(float)(panel.u0()+(panel.u1()-panel.u0())*leftU),u1=(float)(panel.u0()+(panel.u1()-panel.u0())*rightU);
                for(int i=0;i<4;i++)out.addVertex(pose,(float)vertices[i][0],(float)vertices[i][1],(float)vertices[i][2])
                    .setColor(label.primaryColor()).setUv(i<2?u0:u1,i==0||i==3?lowerV:upperV)
                    .setOverlay(overlay).setLight(light).setNormal(pose,0,lying?1:0,lying?0:-1);
            }
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
