package io.vinum.crop;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Natural vine spread is bounded per planted seedling, including its original trellis. */
public final class VineyardConfig {
  public static final int MAX_SECTIONS = 16;
  public static final ModConfigSpec SPEC;
  private static final ModConfigSpec.IntValue MAX_TRELLISES;

  static {
    var builder = new ModConfigSpec.Builder();
    MAX_TRELLISES =
        builder
            .comment(
                "Maximum trellis sections occupied by one seedling, including its original"
                    + " section.",
                "Spread is random along a continuous row; bone meal never causes spreading.")
            .defineInRange("maxTrellisesPerVine", 5, 1, MAX_SECTIONS);
    SPEC = builder.build();
  }

  private VineyardConfig() {}

  public static int maxTrellises() {
    return SPEC.isLoaded() ? MAX_TRELLISES.get() : MAX_TRELLISES.getDefault();
  }
}
