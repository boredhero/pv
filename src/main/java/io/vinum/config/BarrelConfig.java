package io.vinum.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** World-specific capacity and maturation settings, synchronized to joining clients. */
public final class BarrelConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue CAPACITY;
    private static final ModConfigSpec.IntValue TICKS_PER_DAY;
    private static final ModConfigSpec.IntValue REPOSADO_DAYS;
    private static final ModConfigSpec.IntValue ANEJO_DAYS;
    private static final ModConfigSpec.IntValue EXTRA_ANEJO_DAYS;
    private static final ModConfigSpec.IntValue WINE_MATURE_DAYS;
    private static final ModConfigSpec.IntValue WINE_RESERVE_DAYS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("barrels");
        CAPACITY = builder.comment("Capacity of each barrel block in millibuckets (1000 mB = 1 litre).")
                .defineInRange("capacityPerBlock", 225_000, 1000, 2_000_000);
        TICKS_PER_DAY = builder.comment("Elapsed server game ticks counted as one maturation day. Time-of-day commands do not advance aging.")
                .defineInRange("ticksPerDay", 24_000, 1, 24_000_000);
        REPOSADO_DAYS = builder.comment("Minimum tequila barrel age for reposado; default represents two 30-day months.")
                .defineInRange("tequilaReposadoDays", 60, 1, 365_000);
        ANEJO_DAYS = builder.comment("Minimum tequila barrel age for anejo.")
                .defineInRange("tequilaAnejoDays", 365, 1, 365_000);
        EXTRA_ANEJO_DAYS = builder.comment("Minimum tequila barrel age for extra anejo.")
                .defineInRange("tequilaExtraAnejoDays", 1095, 1, 365_000);
        WINE_MATURE_DAYS = builder.comment("Wine gameplay maturation milestone; not a universal real-world wine standard.")
                .defineInRange("wineMatureDays", 180, 1, 365_000);
        WINE_RESERVE_DAYS = builder.comment("Wine gameplay reserve milestone. Aging never increases alcohol strength.")
                .defineInRange("wineReserveDays", 730, 1, 365_000);
        builder.pop();
        SPEC = builder.build();
    }

    private BarrelConfig() {}
    private static int value(ModConfigSpec.IntValue value) { return SPEC.isLoaded() ? value.get() : value.getDefault(); }
    public static int capacityPerBlock() { return value(CAPACITY); }
    public static int ticksPerDay() { return value(TICKS_PER_DAY); }
    public static int tequilaReposadoDays() { return value(REPOSADO_DAYS); }
    public static int tequilaAnejoDays() { return Math.max(tequilaReposadoDays(), value(ANEJO_DAYS)); }
    public static int tequilaExtraAnejoDays() { return Math.max(tequilaAnejoDays(), value(EXTRA_ANEJO_DAYS)); }
    public static int wineMatureDays() { return value(WINE_MATURE_DAYS); }
    public static int wineReserveDays() { return Math.max(wineMatureDays(), value(WINE_RESERVE_DAYS)); }
    public static long daysToTicks(long days) { return Math.multiplyExact(days, (long) ticksPerDay()); }
}
