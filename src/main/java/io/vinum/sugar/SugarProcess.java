package io.vinum.sugar;

import io.vinum.production.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Packaged process definitions: volumes in mB; sugar removed per output item in grams. */
public record SugarProcess(String id, ResourceLocation input, int inputAmount, ResourceLocation output,
        int outputAmount, ResourceLocation solid, double sugarPerSolid, int ticks,
        ResourceLocation liquidIngredient, ResourceLocation solidIngredient) {
    private static final List<SugarProcess> RECIPES = load();
    public static List<SugarProcess> recipes() { return RECIPES; }
    private static List<SugarProcess> load() {
        try (var stream = SugarProcess.class.getResourceAsStream("/vinum/sugar_processes.json")) {
            if (stream == null) throw new IllegalStateException("Missing sugar process definitions");
            var array = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("recipes");
            var result = new ArrayList<SugarProcess>();
            for (var element : array) {
                var o=element.getAsJsonObject();
                var recipe=new SugarProcess(o.get("id").getAsString(),ResourceLocation.parse(o.get("input").getAsString()),o.get("input_amount").getAsInt(),
                    o.has("output")?ResourceLocation.parse(o.get("output").getAsString()):null,o.get("output_amount").getAsInt(),
                    o.has("solid")?ResourceLocation.parse(o.get("solid").getAsString()):null,o.get("sugar_per_solid").getAsDouble(),o.get("ticks").getAsInt(),
                    o.has("liquid_ingredient")?ResourceLocation.parse(o.get("liquid_ingredient").getAsString()):null,
                    o.has("solid_ingredient")?ResourceLocation.parse(o.get("solid_ingredient").getAsString()):null);
                if(recipe.inputAmount<=0 || recipe.outputAmount<0 || recipe.ticks<=0 || recipe.sugarPerSolid<0) throw new IllegalStateException("Invalid sugar process");
                result.add(recipe);
            }
            return List.copyOf(result);
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public int batches(FluidStack fluid) {
        if(fluid.isEmpty() || !BuiltInRegistries.FLUID.getKey(fluid.getFluid()).equals(input) || fluid.getAmount()%inputAmount!=0) return 0;
        var p=BatchProfile.of(fluid);
        if(p.alcoholFraction()!=0 || p.sugarGramsPerLitre()*inputAmount/1000.0+1e-6<sugarPerSolid) return 0;
        // Panela must capture the entire batch's sugar, not destroy an arbitrary remainder.
        if(output==null && Math.abs(p.sugarGramsPerLitre()*inputAmount/1000.0-sugarPerSolid)>1e-6) return 0;
        return fluid.getAmount()/inputAmount;
    }
    public FluidStack liquid(FluidStack inputFluid) {
        int count=batches(inputFluid);
        if(output==null || count==0) return FluidStack.EMPTY;
        var p=BatchProfile.of(inputFluid);
        double sugar=(p.sugarGramsPerLitre()*inputFluid.getAmount()/1000.0-count*sugarPerSolid)*1000.0/(count*outputAmount);
        FluidStack result=new FluidStack(BuiltInRegistries.FLUID.get(output),count*outputAmount);
        result.applyComponents(inputFluid.getComponentsPatch());
        new BatchProfile(Math.max(0,sugar),p.starchGramsPerLitre()*inputAmount/outputAmount,p.solidsGramsPerLitre()*inputAmount/outputAmount,0,liquidIngredient==null?p.ingredientProportions():Map.of(liquidIngredient,1.0),p.yeast(),p.history()).addHistory(ProductionInputs.id(id)).applyTo(result);
        return result;
    }
    public ItemStack solids(FluidStack inputFluid) {
        int count=batches(inputFluid);
        if(solid==null || count==0) return ItemStack.EMPTY;
        ItemStack result=new ItemStack(BuiltInRegistries.ITEM.get(solid),count);
        var p=BatchProfile.of(inputFluid);
        // Solid profile uses a documented 1-litre equivalent: sugar_g_per_litre is grams per item.
        result.set(ProductionDataComponents.BATCH_PROFILE.get(),new BatchProfile(sugarPerSolid,0,output==null?p.solidsGramsPerLitre()*inputAmount/1000.0:0,0,solidIngredient==null?p.ingredientProportions():Map.of(solidIngredient,1.0),Optional.empty(),p.history()).addHistory(ProductionInputs.id(id)));
        return result;
    }
}
