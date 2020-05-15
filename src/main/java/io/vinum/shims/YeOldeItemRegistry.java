package io.vinum.shims;

import io.vinum.common.Defines;
import io.vinum.item.ModItemGroup;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * This class exsits so that items like jkuicer and ice_pick can return themselves as a container when crafted.
 * It is hacky, and not elegant. But the alternative is implementing a custom crafting handler or using Reflection
 * to change the Item.Properties of an item made and registered like the rest of the items via DeferredRegister.
 * These solutions both struck me as inelegant in their own way.
 * And so, LONG LIVE OLD FASHIONED REGISTRY EVENTS!!! DeferredRegister is henceforth....deferred...
 */

public class YeOldeItemRegistry {
    public static Item JUICER;
    public static Item ICE_PICK;
    public static Item MARGARITA_GLASS_MOLD;

    public static void registerAll(RegistryEvent.Register<Item> event){
        if(!event.getName().equals(ForgeRegistries.ITEMS.getRegistryName())){
            return;
        }

        JUICER = register("juicer", new ItemDontBreakOnCraft(new Item.Properties().maxStackSize(1).group(ModItemGroup.MAIN)));
        ICE_PICK = register("ice_pick", new ItemDontBreakOnCraft(new Item.Properties().maxStackSize(1).group(ModItemGroup.MAIN)));
        MARGARITA_GLASS_MOLD = register("margarita_glass_mold", new ItemDontBreakOnCraft(new Item.Properties().maxStackSize(1).group(ModItemGroup.MAIN)));
    }

    private static <T extends Item> T register(String name, T item){
        ResourceLocation id = (new ResourceLocation(Defines.MODID, name));
        item.setRegistryName(id);
        ForgeRegistries.ITEMS.register(item);
        return item;
    }

}