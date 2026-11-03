package io.vinum.blender;

import io.vinum.beverage.FluidBlending;
import io.vinum.production.*;
import io.vinum.sugar.SugarContent;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Pure batch planning. Solid profiles use grams per item; fluid profiles use grams per litre. */
public final class BlenderProcess {
    public record Plan(int amountA,int amountB,FluidStack output,int ingredients,ItemStack remainder){}
    private BlenderProcess(){}
    public static int ingredientsRequired(int mode,ItemStack ingredient){
        if(mode==BlenderBlockEntity.HONEY)return ingredient.is(Items.HONEY_BOTTLE)?1:0;
        if(mode==BlenderBlockEntity.WASH)return ingredient.is(Items.SUGAR)?2:ingredient.is(SugarContent.PANELA.get())?1:0;
        return 0;
    }
    public static boolean plainWater(FluidStack fluid){return !fluid.isEmpty()&&fluid.getFluid().isSame(Fluids.WATER)&&fluid.isComponentsPatchEmpty();}
    public static Plan plan(int mode,int ratio,FluidStack a,FluidStack b,ItemStack ingredient){return plan(mode,ratio,400,a,b,ingredient);}
    public static Plan plan(int mode,int ratio,int targetTenths,FluidStack a,FluidStack b,ItemStack ingredient){
        if(mode==BlenderBlockEntity.TARGET_ABV)return targetPlan(targetTenths,a,b);
        if(mode==BlenderBlockEntity.BLEND){
            if(ratio<0||ratio>100)return null;
            int divisor=gcd(ratio,100-ratio),unitA=ratio/divisor,unitB=(100-ratio)/divisor;
            int units=1000/(unitA+unitB);
            if(unitA>0)units=Math.min(units,a.getAmount()/unitA);
            if(unitB>0)units=Math.min(units,b.getAmount()/unitB);
            if(units<=0)return null;
            var partA=unitA==0?FluidStack.EMPTY:a.copyWithAmount(unitA*units);
            var partB=unitB==0?FluidStack.EMPTY:b.copyWithAmount(unitB*units);
            if(!FluidBlending.canBlend(partA,partB))return null;
            return new Plan(partA.getAmount(),partB.getAmount(),FluidBlending.blend(partA,partB),0,ItemStack.EMPTY);
        }
        if(!plainWater(a)||a.getAmount()<1000)return null;
        int count=ingredientsRequired(mode,ingredient);
        if(count==0||ingredient.getCount()<count)return null;
        if(mode==BlenderBlockEntity.HONEY){
            // Both still and flowing plain water represent the same measured ingredient.
            var must=HoneyMixing.prepare(new FluidStack(Fluids.WATER,1000));
            return must.isEmpty()?null:new Plan(1000,0,must,1,new ItemStack(Items.GLASS_BOTTLE));
        }
        if(mode!=BlenderBlockEntity.WASH)return null;
        var profile=ingredient.get(ProductionDataComponents.BATCH_PROFILE.get());
        if(profile==null&&ingredient.is(SugarContent.PANELA.get()))profile=new BatchProfile(200,0,0,0,Map.of(ProductionInputs.id("panela"),1D),Optional.empty(),List.of());
        if(profile==null||profile.ingredientProportions().isEmpty()||profile.sugarGramsPerLitre()<=0||profile.alcoholFraction()!=0)return null;
        double sugar=profile.sugarGramsPerLitre()*count,starch=profile.starchGramsPerLitre()*count,solids=profile.solidsGramsPerLitre()*count;
        if(sugar>10000||starch>10000||solids>10000)return null;
        var wash=new FluidStack(io.vinum.fluid.ModFluids.CANE_WASH.source().get(),1000);
        new BatchProfile(sugar,starch,solids,0,profile.ingredientProportions(),profile.yeast(),profile.history())
            .addHistory(ProductionInputs.id("dissolving")).applyTo(wash);
        return new Plan(1000,0,wash,count,ItemStack.EMPTY);
    }
    /** Select an integer-millibucket blend; the resulting profile always uses actual conserved alcohol. */
    private static Plan targetPlan(int targetTenths,FluidStack a,FluidStack b){
        if(targetTenths<1||targetTenths>950||a.isEmpty()||!plainWater(b)||!FluidBlending.canBlend(a,b))return null;
        double source=BatchProfile.of(a).alcoholFraction(),target=targetTenths/1000D;
        if(source<=target)return null;
        int bestA=0,bestB=0;
        double bestError=Double.POSITIVE_INFINITY;
        int maximum=(int)Math.min(1000,(long)a.getAmount()+b.getAmount());
        for(int total=2;total<=maximum;total++){
            int amountA=(int)Math.round(total*target/source),amountB=total-amountA;
            if(amountA<1||amountB<1||amountA>a.getAmount()||amountB>b.getAmount())continue;
            double error=Math.abs(source*amountA/total-target);
            if(error<bestError-1e-12||Math.abs(error-bestError)<=1e-12&&total>bestA+bestB){bestA=amountA;bestB=amountB;bestError=error;}
        }
        if(bestA==0||bestError>0.0005)return null;
        return new Plan(bestA,bestB,FluidBlending.blend(a.copyWithAmount(bestA),b.copyWithAmount(bestB)),0,ItemStack.EMPTY);
    }
    private static int gcd(int a,int b){while(b!=0){int t=a%b;a=b;b=t;}return a;}
}
