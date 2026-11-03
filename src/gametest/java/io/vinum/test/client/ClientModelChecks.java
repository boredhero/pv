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
    }
}
