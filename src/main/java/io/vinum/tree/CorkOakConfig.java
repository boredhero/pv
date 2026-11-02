package io.vinum.tree;

import net.neoforged.neoforge.common.ModConfigSpec;

/** A deliberate gameplay compression of the real nine-year cork harvest cycle. */
public final class CorkOakConfig {
  public static final ModConfigSpec SPEC;
  private static final ModConfigSpec.IntValue REGROWTH_TICKS;

  static {
    var builder = new ModConfigSpec.Builder();
    REGROWTH_TICKS =
        builder
            .comment(
                "Server game ticks for cork bark to regenerate. Default: nine Minecraft days"
                    + " (216000 ticks).",
                "This compresses the real minimum nine-year cycle for gameplay. Time-of-day"
                    + " commands do not advance this clock.")
            .defineInRange("corkRegrowthTicks", 216000, 20, 2_000_000_000);
    SPEC = builder.build();
  }

  private CorkOakConfig() {}

  public static int regrowthTicks() {
    return SPEC.isLoaded() ? REGROWTH_TICKS.get() : REGROWTH_TICKS.getDefault();
  }
}
