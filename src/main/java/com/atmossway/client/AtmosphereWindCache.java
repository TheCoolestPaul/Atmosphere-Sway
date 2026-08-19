package com.atmossway.client;

import com.atmossway.config.AtmosSwayConfig;
import com.atmossway.network.ClientWindSyncState;
import com.atmossway.network.WindSyncPayload;
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

    static Sample sample(ClientLevel level, Player player, boolean integratedServer) {
        long gameTime = level.getGameTime();
        RegionInstanceKey region = RegionInstanceKey.from(player.blockPosition());
        int interval = AtmosSwayConfig.SAMPLE_INTERVAL_TICKS.get();
        boolean tickWentBackwards = gameTime < lastSampleTick;
        boolean intervalElapsed = tickWentBackwards || gameTime - lastSampleTick >= interval;
        boolean regionChanged = lastRegion == null || !lastRegion.equals(region);

        if (regionChanged || intervalElapsed) {
            ClientWindSyncState.Selection synchronizedWind = ClientWindSyncState.select(
                    level.dimension().location(), region, gameTime
            );
            Source source = selectSource(synchronizedWind.available(), integratedServer);
            float speedMps = 0.0F;
            float directionDeg = 0.0F;
            boolean valid = false;
            if (source == Source.SERVER_SYNC) {
                WindSyncPayload payload = synchronizedWind.payload();
                speedMps = payload.speedMps();
                directionDeg = payload.directionDeg();
                valid = payload.valid();
            } else if (source == Source.INTEGRATED_DIRECT) {
                WindVectorApi.WindSample direct = WindVectorApi.getSurface(region, gameTime);
                speedMps = direct.speedMps();
                directionDeg = direct.directionDeg();
                valid = Float.isFinite(speedMps)
                        && Float.isFinite(directionDeg)
                        && speedMps >= 0.0F;
            }
            WindForceMath.WindForce force = WindForceMath.fromAtmosphere(
                    speedMps,
                    directionDeg,
                    AtmosSwayConfig.WIND_STRENGTH_SCALE.get(),
                    AtmosSwayConfig.MAX_WIND_INTENSITY.get()
            );
            cached = new Sample(
                    region, speedMps, directionDeg, force, valid, regionChanged,
                    source, synchronizedWind.ageTicks(), synchronizedWind.match(),
                    synchronizedWind.receivedCount(), synchronizedWind.rejectedCount()
            );
            AtmosSwayDiagnostics.windSample(
                    region, gameTime, speedMps, directionDeg, force, valid,
                    source.diagnosticName(), synchronizedWind.ageTicks(), synchronizedWind.match(),
                    synchronizedWind.receivedCount(), synchronizedWind.rejectedCount()
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

    static Source selectSource(boolean synchronizedAvailable, boolean integratedServer) {
        if (synchronizedAvailable) {
            return Source.SERVER_SYNC;
        }
        return integratedServer ? Source.INTEGRATED_DIRECT : Source.UNAVAILABLE;
    }

    enum Source {
        SERVER_SYNC("server_sync"),
        INTEGRATED_DIRECT("integrated_direct"),
        UNAVAILABLE("unavailable");

        private final String diagnosticName;

        Source(String diagnosticName) {
            this.diagnosticName = diagnosticName;
        }

        String diagnosticName() {
            return diagnosticName;
        }
    }

    record Sample(RegionInstanceKey region, float speedMps, float directionDeg,
                  WindForceMath.WindForce force, boolean valid, boolean regionChanged,
                  Source source, long syncAgeTicks, String syncMatch,
                  long packetsReceived, long packetsRejected) {
        private static final Sample NONE = new Sample(
                null, 0.0F, 0.0F, WindForceMath.WindForce.NONE, false, false,
                Source.UNAVAILABLE, Long.MAX_VALUE, "missing", 0L, 0L
        );

        private Sample withRegionChanged(boolean changed) {
            return new Sample(
                    region, speedMps, directionDeg, force, valid, changed,
                    source, syncAgeTicks, syncMatch, packetsReceived, packetsRejected
            );
        }
    }
}
