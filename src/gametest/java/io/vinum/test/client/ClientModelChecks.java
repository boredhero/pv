package io.vinum.test.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-only regression check: generated sprite parents silently discard native tool geometry. */
@EventBusSubscriber(modid = "vinum_tests", value = Dist.CLIENT)
public final class ClientModelChecks {
    @SubscribeEvent
    public static void baked(ModelEvent.BakingCompleted event) {
        if (!Boolean.getBoolean("vinum.clientTests")) return;
        for (String name : new String[] {"coa", "airlock", "carboy", "grape_trellis", "bar", "barstool", "shot_glass", "wine_glass_empty", "margarita_glass", "spiced_apple_margarita", "cinnamon_log", "cinnamon_wood", "stripped_cinnamon_log", "stripped_cinnamon_wood", "cinnamon_leaves", "cinnamon_sapling", "cinnamon_planks", "cinnamon_stairs", "cinnamon_slab", "cinnamon_fence", "cinnamon_fence_gate", "cinnamon_door", "cinnamon_trapdoor", "cinnamon_button", "cinnamon_pressure_plate"}) {
            var id = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("vinum", name));
            var model = event.getModels().get(id);
            if (model == null || model == event.getModelManager().getMissingModel())
                throw new AssertionError("Missing inventory model: " + id);
            int quads = model.getQuads(null, null, RandomSource.create(0), net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
            for (Direction side : Direction.values())
                quads += model.getQuads(null, side, RandomSource.create(0), net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
            if (quads == 0) throw new AssertionError("Invisible inventory model: " + id);
            if (java.util.Set.of("airlock", "carboy", "grape_trellis").contains(name)) {
                var guiPose = new com.mojang.blaze3d.vertex.PoseStack();
                var gui = model.applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI, guiPose, false);
                if (!guiPose.last().pose().equals(new org.joml.Matrix4f(), 0.00001F))
                    throw new AssertionError("Flat icon inherited a block GUI transform: " + id);
                var guiQuads = gui.getQuads(null, null, RandomSource.create(0),
                        net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                if (gui == model || guiQuads.size() != 2)
                    throw new AssertionError("Expected separate flat GUI sprite: " + id);
                if (model.applyTransform(net.minecraft.world.item.ItemDisplayContext.NONE,
                        new com.mojang.blaze3d.vertex.PoseStack(), false) == gui)
                    throw new AssertionError("Placed model incorrectly uses GUI sprite: " + id);
                for (var quad : guiQuads)
                    if (quad.getSprite().contents().name().getPath().equals("missingno"))
                        throw new AssertionError("Missing GUI texture: " + id);
            }
            LogUtils.getLogger().info("Vinum client model check: {} has {} rendered quads", name, quads);
        }
        BarRenderChecks.run();
        KettleRenderChecks.run();
        wineBottles(event);
        newBottles(event);
    }

    private static void wineBottles(ModelEvent.BakingCompleted event) {
        var bottle=(io.vinum.item.FluidContainerItem)io.vinum.item.ModItems.WINE_BOTTLE_EMPTY.get();
        var base=event.getModels().get(ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("vinum","wine_bottle_empty")));
        if(base==null || base==event.getModelManager().getMissingModel())throw new AssertionError("Missing canonical wine bottle model");
        var variants=io.vinum.beverage.WineCatalog.varieties();
        var colors=new java.util.HashSet<Integer>();
        for(var variety:variants) {
            var fluid=io.vinum.beverage.WineCatalog.sample(variety,750);
            var stack=bottle.filled(fluid);
            var model=base.getOverrides().resolve(base,stack,null,null,0);
            if(model==null || model==base || model==event.getModelManager().getMissingModel())
                throw new AssertionError("Filled wine did not select layered inventory model: "+variety.ingredient());
            var quads=model.getQuads(null,null,RandomSource.create(0),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,null);
            if(quads.stream().noneMatch(q->q.getTintIndex()==255))throw new AssertionError("Wine model has no liquid tint layer");
            if(quads.stream().noneMatch(q->q.getTintIndex()<0))throw new AssertionError("Wine label must preserve original artwork colors");
            int color=io.vinum.beverage.WineCatalog.color(fluid);
            if(color!=variety.color())throw new AssertionError("Bottle appearance overrides varietal wine color");
            colors.add(color);
        }
        if(colors.size()<2)throw new AssertionError("Wine varieties lost their distinct colors");
        LogUtils.getLogger().info("Vinum wine inventory checks passed: {} varieties select layered models and preserve batch colors",variants.size());
    }
    private static void newBottles(ModelEvent.BakingCompleted event) {
        for(String name:new String[]{"mead","honeyshine","brandy"}) {
            var itemId=ResourceLocation.fromNamespaceAndPath("vinum",name+"_bottle");
            var bottle=(io.vinum.item.FluidContainerItem)net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId);
            var base=event.getModels().get(ModelResourceLocation.inventory(itemId));
            if(base==null || base==event.getModelManager().getMissingModel())throw new AssertionError("Missing bottle model: "+itemId);
            var fluid=net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("vinum",name));
            for(int amount:new int[]{1,187,375,562,750}) for(int days:new int[]{0,90,180,365}) {
                var contents=io.vinum.barrel.BarrelFluidData.withAge(io.vinum.item.ModItems.spirit(fluid,amount),days*24000L);
                var stack=bottle.filled(contents);var model=base.getOverrides().resolve(base,stack,null,null,0);
                if(model==null || model==base || model==event.getModelManager().getMissingModel())throw new AssertionError("Unresolved filled model: "+itemId);
                var quads=model.getQuads(null,null,RandomSource.create(0),net.neoforged.neoforge.client.model.data.ModelData.EMPTY,null);
                if(quads.stream().noneMatch(q->q.getTintIndex()==255))throw new AssertionError("Missing liquid tint: "+itemId);
                if(quads.stream().noneMatch(q->q.getTintIndex()<0))throw new AssertionError("Missing untinted artwork: "+itemId);
                if(quads.stream().anyMatch(q->q.getSprite().contents().name().getPath().equals("missingno")))throw new AssertionError("Missing bottle texture: "+itemId);
            }
        }
        LogUtils.getLogger().info("Vinum mead/honeyshine/brandy inventory checks passed: 60 fill and maturity combinations");
    }
}
