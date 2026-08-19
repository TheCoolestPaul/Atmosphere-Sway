package com.atmossway.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AtmosSwayConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ModConfigSpec.DoubleValue WIND_STRENGTH_SCALE;
    public static final ModConfigSpec.DoubleValue MAX_WIND_INTENSITY;
    public static final ModConfigSpec.IntValue SAMPLE_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Client-side Project Atmosphere wind integration for SWAY.")
                .push("wind");

        ENABLED = builder
                .comment("Enable Project Atmosphere wind deformation.")
                .define("enabled", true);
        DEBUG_LOGGING = builder
                .comment("Log a rate-limited diagnostic summary for wind sampling and foliage rendering.")
                .define("debugLogging", false);
        WIND_STRENGTH_SCALE = builder
                .comment("SWAY force contributed per metre per second of surface wind.")
                .defineInRange("windStrengthScale", 0.08D, 0.0D, 1.0D);
        MAX_WIND_INTENSITY = builder
                .comment("Maximum SWAY intensity contributed by wind.")
                .defineInRange("maxWindIntensity", 1.5D, 0.0D, 5.0D);
        SAMPLE_INTERVAL_TICKS = builder
                .comment("How often Project Atmosphere wind is sampled, in client ticks.")
                .defineInRange("sampleIntervalTicks", 1, 1, 200);

        builder.pop();
        SPEC = builder.build();
    }

    private AtmosSwayConfig() {
    }
}
