package com.atmossway.client;

import com.atmossway.config.AtmosSwayConfig;
import org.joml.Vector3f;

public final class ParticleRainWindAdapter {
    private ParticleRainWindAdapter() {
    }

    public static Vector3f overrideWind() {
        Vector3f wind = overrideWind(
                AtmosSwayConfig.ENABLED.get(),
                AtmosSwayConfig.PARTICLE_RAIN_ADAPTER.get(),
                PrecipitationWindController.tickPose(),
                AtmosSwayConfig.PARTICLE_RAIN_WIND_SCALE.get().floatValue()
        );
        AtmosSwayDiagnostics.particleRainAdapterState(wind != null);
        if (wind != null) {
            AtmosSwayDiagnostics.particleRainWindOverride();
        }
        return wind;
    }

    static Vector3f overrideWind(boolean globallyEnabled, boolean adapterEnabled,
                                 PrecipitationWindController.Pose pose, float scale) {
        if (!globallyEnabled || !adapterEnabled || pose == null || !pose.active()) {
            return null;
        }
        return windVector(pose.speedMps(), pose.headingRadians(), scale);
    }

    static Vector3f windVector(float speedMps, float headingRadians, float scale) {
        if (!Float.isFinite(speedMps) || speedMps < 0.0F
                || !Float.isFinite(headingRadians)
                || !Float.isFinite(scale) || scale < 0.0F) {
            return new Vector3f();
        }
        float velocity = speedMps * scale;
        if (!Float.isFinite(velocity)) {
            return new Vector3f();
        }
        return new Vector3f(
                (float) -Math.sin(headingRadians) * velocity,
                0.0F,
                (float) Math.cos(headingRadians) * velocity
        );
    }
}
