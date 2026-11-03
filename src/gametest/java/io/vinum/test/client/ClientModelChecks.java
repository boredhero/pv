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
        for (String name : new String[] {"coa", "airlock", "carboy"}) {
            var id = ModelResourceLocation.inventory(ResourceLocation.fromNamespaceAndPath("vinum", name));
            var model = event.getModels().get(id);
            if (model == null || model == event.getModelManager().getMissingModel())
                throw new AssertionError("Missing inventory model: " + id);
            int quads = model.getQuads(null, null, RandomSource.create(0), net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
            for (Direction side : Direction.values())
                quads += model.getQuads(null, side, RandomSource.create(0), net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null).size();
            if (quads == 0) throw new AssertionError("Invisible inventory model: " + id);
            LogUtils.getLogger().info("Vinum client model check: {} has {} rendered quads", name, quads);
        }
    }
}
