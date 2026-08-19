package com.atmossway.client;

public final class WindForceMath {
    private WindForceMath() {
    }

    public static WindForce fromAtmosphere(float speedMps, float directionDeg,
                                           double strengthScale, double maximumIntensity) {
        if (!Float.isFinite(speedMps) || !Float.isFinite(directionDeg)
                || !Double.isFinite(strengthScale) || !Double.isFinite(maximumIntensity)
                || speedMps <= 0.0F || strengthScale <= 0.0D || maximumIntensity <= 0.0D) {
            return WindForce.NONE;
        }

        float radians = (float) Math.toRadians(directionDeg);
        float x = (float) -Math.sin(radians);
        float z = (float) Math.cos(radians);
        float intensity = (float) Math.min(speedMps * strengthScale, maximumIntensity);
        if (!Float.isFinite(intensity) || intensity <= 0.001F) {
            return WindForce.NONE;
        }
        return new WindForce(x, z, intensity);
    }

    public static WindForce vectorSum(WindForce first, WindForce second) {
        float x = first.x() * first.intensity() + second.x() * second.intensity();
        float z = first.z() * first.intensity() + second.z() * second.intensity();
        float intensity = (float) Math.sqrt(x * x + z * z);
        if (intensity <= 0.001F) {
            return WindForce.NONE;
        }
        return new WindForce(x / intensity, z / intensity, intensity);
    }

    public static WindForce vectorSumCapped(WindForce first, WindForce second, float maximumIntensity) {
        WindForce combined = vectorSum(first, second);
        if (!combined.isPresent() || !Float.isFinite(maximumIntensity) || maximumIntensity <= 0.0F) {
            return WindForce.NONE;
        }
        if (combined.intensity() <= maximumIntensity) {
            return combined;
        }
        return new WindForce(combined.x(), combined.z(), maximumIntensity);
    }

    public static float vectorDelta(WindForce first, WindForce second) {
        float dx = first.x() * first.intensity() - second.x() * second.intensity();
        float dz = first.z() * first.intensity() - second.z() * second.intensity();
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean needsRefresh(WindForce current, WindForce rendered,
                                       boolean enabled, boolean wasEnabled,
                                       float threshold) {
        return enabled != wasEnabled || vectorDelta(current, rendered) >= threshold;
    }

    public record WindForce(float x, float z, float intensity) {
        public static final WindForce NONE = new WindForce(0.0F, 0.0F, 0.0F);

        public boolean isPresent() {
            return intensity > 0.001F;
        }
    }
}
