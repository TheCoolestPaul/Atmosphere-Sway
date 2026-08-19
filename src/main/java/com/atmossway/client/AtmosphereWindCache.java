package com.atmossway.client;

import com.atmossway.config.AtmosSwayConfig;
import net.Gabou.projectatmosphere.api.WindVectorApi;
import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;

final class AtmosphereWindCache {
    private static long lastSampleTick = Long.MIN_VALUE;
    private static RegionInstanceKey lastRegion;
    private static WindForceMath.WindForce cached = WindForceMath.WindForce.NONE;

    private AtmosphereWindCache() {
    }

    static WindForceMath.WindForce sample(ClientLevel level, Player player) {
        if (!AtmosSwayConfig.ENABLED.get()) {
            cached = WindForceMath.WindForce.NONE;
            return cached;
        }

        long gameTime = level.getGameTime();
        RegionInstanceKey region = RegionInstanceKey.from(player.blockPosition());
        int interval = AtmosSwayConfig.SAMPLE_INTERVAL_TICKS.get();
        boolean tickWentBackwards = gameTime < lastSampleTick;
        boolean intervalElapsed = tickWentBackwards || gameTime - lastSampleTick >= interval;

        if (lastRegion == null || !lastRegion.equals(region) || intervalElapsed) {
            WindVectorApi.WindSample sample = WindVectorApi.getSurface(region, gameTime);
            cached = WindForceMath.fromAtmosphere(
                    sample.speedMps(),
                    sample.directionDeg(),
                    AtmosSwayConfig.WIND_STRENGTH_SCALE.get(),
                    AtmosSwayConfig.MAX_WIND_INTENSITY.get()
            );
            lastRegion = region;
            lastSampleTick = gameTime;
        }
        return cached;
    }

    static void reset() {
        lastSampleTick = Long.MIN_VALUE;
        lastRegion = null;
        cached = WindForceMath.WindForce.NONE;
    }
}
