package io.vinum.cinnamon;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

/** True cinnamon shares ordinary wood mechanics; its bark has a single harvest per log. */
public final class CinnamonContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,"vinum");
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("vinum");
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"vinum");
    public static final ResourceKey<ConfiguredFeature<?,?>> TREE_KEY=ResourceKey.create(Registries.CONFIGURED_FEATURE,id("cinnamon_tree"));
    private static final TreeGrower GROWER=new TreeGrower("vinum:cinnamon",Optional.empty(),Optional.of(TREE_KEY),Optional.empty());
    public static final DeferredHolder<Block,Block> STRIPPED_LOG=block("stripped_cinnamon_log",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_LOG)));
    public static final DeferredHolder<Block,Block> STRIPPED_WOOD=block("stripped_cinnamon_wood",()->new RotatedPillarBlock(copy(Blocks.STRIPPED_OAK_WOOD)));
    public static final DeferredHolder<Block,Block> LOG=block("cinnamon_log",()->new CinnamonLogBlock(copy(Blocks.OAK_LOG),STRIPPED_LOG));
    public static final DeferredHolder<Block,Block> WOOD=block("cinnamon_wood",()->new CinnamonLogBlock(copy(Blocks.OAK_WOOD),STRIPPED_WOOD));
    public static final DeferredHolder<Block,Block> PLANKS=wood("planks",()->new Block(copy(Blocks.OAK_PLANKS)));
    public static final DeferredHolder<Block,Block> STAIRS=wood("stairs",()->new StairBlock(PLANKS.get().defaultBlockState(),copy(Blocks.OAK_STAIRS)));
    public static final DeferredHolder<Block,Block> SLAB=wood("slab",()->new SlabBlock(copy(Blocks.OAK_SLAB)));
    public static final DeferredHolder<Block,Block> FENCE=wood("fence",()->new FenceBlock(copy(Blocks.OAK_FENCE)));
    public static final DeferredHolder<Block,Block> FENCE_GATE=wood("fence_gate",()->new FenceGateBlock(WoodType.OAK,copy(Blocks.OAK_FENCE_GATE)));
    public static final DeferredHolder<Block,Block> DOOR=wood("door",()->new DoorBlock(BlockSetType.OAK,copy(Blocks.OAK_DOOR)));
    public static final DeferredHolder<Block,Block> TRAPDOOR=wood("trapdoor",()->new TrapDoorBlock(BlockSetType.OAK,copy(Blocks.OAK_TRAPDOOR)));
    public static final DeferredHolder<Block,Block> BUTTON=wood("button",()->new ButtonBlock(BlockSetType.OAK,30,copy(Blocks.OAK_BUTTON)));
    public static final DeferredHolder<Block,Block> PRESSURE_PLATE=wood("pressure_plate",()->new PressurePlateBlock(BlockSetType.OAK,copy(Blocks.OAK_PRESSURE_PLATE)));
    public static final DeferredHolder<Block,Block> LEAVES=wood("leaves",()->new LeavesBlock(copy(Blocks.OAK_LEAVES)));
    public static final DeferredHolder<Block,Block> SAPLING=wood("sapling",()->new SaplingBlock(GROWER,copy(Blocks.OAK_SAPLING)));
    public static final DeferredHolder<Feature<?>,CinnamonTreeFeature> TREE_FEATURE=FEATURES.register("cinnamon_tree",CinnamonTreeFeature::new);
    private CinnamonContent(){}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("vinum",path);}
    private static BlockBehaviour.Properties copy(Block block){return BlockBehaviour.Properties.ofFullCopy(block);}
    private static DeferredHolder<Block,Block> wood(String suffix,Supplier<Block> supplier){return block("cinnamon_"+suffix,supplier);}
    private static DeferredHolder<Block,Block> block(String name,Supplier<Block> supplier){
        var result=BLOCKS.register(name,supplier);
        ITEMS.register(name,()->new BlockItem(result.get(),new Item.Properties()));return result;
    }
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);FEATURES.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event)->{
            if(event.getTab()==io.vinum.item.ModItemGroup.MAIN.get())ITEMS.getEntries().forEach(item->event.accept(item.get()));
        });
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            var fire=(FireBlock)Blocks.FIRE;
            for(var log:List.of(LOG,WOOD,STRIPPED_LOG,STRIPPED_WOOD))fire.setFlammable(log.get(),5,5);
            fire.setFlammable(LEAVES.get(),30,60);
            for(var wood:List.of(PLANKS,STAIRS,SLAB,FENCE,FENCE_GATE,DOOR,TRAPDOOR,BUTTON,PRESSURE_PLATE))fire.setFlammable(wood.get(),5,20);
            ComposterBlock.COMPOSTABLES.put(SAPLING.get().asItem(),.3f);
            ComposterBlock.COMPOSTABLES.put(LEAVES.get().asItem(),.3f);
        }));
    }
}
