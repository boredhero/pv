package io.vinum.worldgen;

import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Wild plants use the same mature geometry and harvest rules as cultivated agave. */
public final class AgaveWorldgen {
  private static final DeferredRegister<Feature<?>> FEATURES =
      DeferredRegister.create(Registries.FEATURE, Defines.MODID);

  static {
    FEATURES.register("mature_agave", MatureAgaveFeature::new);
  }

  private AgaveWorldgen() {}

  public static void register(IEventBus bus) {
    FEATURES.register(bus);
  }
}
