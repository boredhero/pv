package io.vinum.grain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.*;

public final class RyeCornContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"vinum");
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("vinum");
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT=DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,"vinum");
    public static final DeferredHolder<Block,RyeCropBlock> RYE_CROP=BLOCKS.register("crop_rye",()->new RyeCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredHolder<Block,CornCropBlock> CORN_CROP=BLOCKS.register("crop_corn",()->new CornCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> RYE=ITEMS.registerSimpleItem("rye");
    public static final DeferredItem<Item> CORN=ITEMS.registerSimpleItem("corn",new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(3).saturationModifier(.3F).build()));
    public static final DeferredItem<ItemNameBlockItem> RYE_SEEDS=ITEMS.register("rye_seeds",()->new ItemNameBlockItem(RYE_CROP.get(),new Item.Properties()));
    public static final DeferredItem<ItemNameBlockItem> CORN_SEEDS=ITEMS.register("corn_seeds",()->new ItemNameBlockItem(CORN_CROP.get(),new Item.Properties()));
    static {LOOT.register("grass_grain_seeds",()->GrassSeedLootModifier.CODEC);}
    private RyeCornContent(){}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);LOOT.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTab()==io.vinum.item.ModItemGroup.MAIN.get())ITEMS.getEntries().forEach(item->event.accept(item.get()));
        });
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            ComposterBlock.COMPOSTABLES.put(RYE_SEEDS.get(),.3F);ComposterBlock.COMPOSTABLES.put(CORN_SEEDS.get(),.3F);
            ComposterBlock.COMPOSTABLES.put(RYE.get(),.65F);ComposterBlock.COMPOSTABLES.put(CORN.get(),.65F);
        }));
    }
}
