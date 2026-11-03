package io.vinum.beverage;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.drinks.DrinkItem;

/** Vessel presentation is separate from beverage presentation and never replaces inventory models. */
public record DrinkwareAppearance(BottleGeometry geometry,int glassColor,int rimColor,boolean cocktail) {
    private static final Map<ResourceLocation,DrinkwareAppearance> TYPES=load();
    private static Map<ResourceLocation,DrinkwareAppearance> load() {
        var result=new HashMap<ResourceLocation,DrinkwareAppearance>();
        try(var stream=DrinkwareAppearance.class.getResourceAsStream("/vinum/drinkware_appearances.json")) {
            if(stream==null)throw new IllegalStateException("Missing drinkware appearances");
            for(var entry:JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonArray()) {
                var json=entry.getAsJsonObject();float opacity=json.get("opacity").getAsFloat();
                if(!Float.isFinite(opacity)||opacity<0||opacity>1)throw new IllegalArgumentException("Invalid drinkware opacity");
                var style=new DrinkwareAppearance(BottleGeometry.byId(ResourceLocation.parse(json.get("geometry").getAsString())),
                    ((int)(opacity*255)<<24)|Integer.parseInt(json.get("glassTint").getAsString(),16),
                    0xFF000000|Integer.parseInt(json.get("rimColor").getAsString(),16),json.get("cocktail").getAsBoolean());
                for(var item:json.getAsJsonArray("items"))if(result.putIfAbsent(ResourceLocation.parse(item.getAsString()),style)!=null)
                    throw new IllegalArgumentException("Duplicate drinkware appearance");
            }
            return Map.copyOf(result);
        }catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static Optional<DrinkwareAppearance> find(ItemStack stack){return Optional.ofNullable(TYPES.get(BuiltInRegistries.ITEM.getKey(stack.getItem())));}
    public static boolean canDrink(ItemStack stack){return find(stack).isPresent() &&
        (stack.getItem() instanceof FluidContainerItem container?container.canDrink(stack):stack.getItem() instanceof DrinkItem);}
}
