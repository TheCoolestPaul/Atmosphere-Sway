package com.atmossway.client;

import com.atmossway.config.AtmosSwayConfig;
import net.Gabou.projectatmosphere.api.WindVectorApi;
import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

final class AtmosphereWindCache {
    private static long lastSampleTick = Long.MIN_VALUE;
    private static RegionInstanceKey lastRegion;
    private static Sample cached = Sample.NONE;

    private AtmosphereWindCache() {
    }

    static Sample sample(ClientLevel level, Player player) {
        long gameTime = level.getGameTime();
        RegionInstanceKey region = RegionInstanceKey.from(player.blockPosition());
        int interval = AtmosSwayConfig.SAMPLE_INTERVAL_TICKS.get();
        boolean tickWentBackwards = gameTime < lastSampleTick;
        boolean intervalElapsed = tickWentBackwards || gameTime - lastSampleTick >= interval;
        boolean regionChanged = lastRegion == null || !lastRegion.equals(region);

        if (regionChanged || intervalElapsed) {
            WindVectorApi.WindSample sample = WindVectorApi.getSurface(region, gameTime);
            boolean valid = Float.isFinite(sample.speedMps())
                    && Float.isFinite(sample.directionDeg())
                    && sample.speedMps() >= 0.0F;
            WindForceMath.WindForce force = WindForceMath.fromAtmosphere(
                    sample.speedMps(),
                    sample.directionDeg(),
                    AtmosSwayConfig.WIND_STRENGTH_SCALE.get(),
                    AtmosSwayConfig.MAX_WIND_INTENSITY.get()
            );
            cached = new Sample(region, sample.speedMps(), sample.directionDeg(), force, valid, regionChanged);
            AtmosSwayDiagnostics.windSample(
                    region, gameTime, sample.speedMps(), sample.directionDeg(), force, valid
            );
            lastRegion = region;
            lastSampleTick = gameTime;
        } else if (cached.regionChanged()) {
            cached = cached.withRegionChanged(false);
        }
        return cached;
    }

    static void reset() {
        lastSampleTick = Long.MIN_VALUE;
        lastRegion = null;
        cached = Sample.NONE;
    }

    record Sample(RegionInstanceKey region, float speedMps, float directionDeg,
                  WindForceMath.WindForce force, boolean valid, boolean regionChanged) {
        private static final Sample NONE = new Sample(
                null, 0.0F, 0.0F, WindForceMath.WindForce.NONE, false, false
        );

        private Sample withRegionChanged(boolean changed) {
            return new Sample(region, speedMps, directionDeg, force, valid, changed);
        }
    }
}
