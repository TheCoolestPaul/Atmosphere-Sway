package com.atmossway.client;

final class FastGustFilter {
    static final float SMOOTHING_FACTOR = 0.10F;
    static final float RESPONSE_BLEND = 0.50F;
    static final float MAX_ADJUSTMENT = 0.10F;

    private float smoothedX;
    private float smoothedZ;
    private boolean initialized;

    void initialize(WindForceMath.WindForce raw) {
        smoothedX = raw.forceX();
        smoothedZ = raw.forceZ();
        initialized = true;
    }

    void update(WindForceMath.WindForce raw, boolean valid) {
        if (!valid) {
            return;
        }
        if (!initialized) {
            initialize(raw);
            return;
        }
        smoothedX += (raw.forceX() - smoothedX) * SMOOTHING_FACTOR;
        smoothedZ += (raw.forceZ() - smoothedZ) * SMOOTHING_FACTOR;
    }

    Snapshot snapshot(WindForceMath.WindForce committed, float maximumIntensity) {
        if (!initialized) {
            return new Snapshot(
                    WindForceMath.WindForce.NONE,
                    WindForceMath.WindForce.NONE,
                    committed
            );
        }

        float adjustmentX = (smoothedX - committed.forceX()) * RESPONSE_BLEND;
        float adjustmentZ = (smoothedZ - committed.forceZ()) * RESPONSE_BLEND;
        float adjustmentMagnitude = magnitude(adjustmentX, adjustmentZ);
        if (adjustmentMagnitude > MAX_ADJUSTMENT) {
            float scale = MAX_ADJUSTMENT / adjustmentMagnitude;
            adjustmentX *= scale;
            adjustmentZ *= scale;
        }

        WindForceMath.WindForce smoothed = WindForceMath.fromComponents(smoothedX, smoothedZ);
        WindForceMath.WindForce adjustment = WindForceMath.fromComponents(adjustmentX, adjustmentZ);
        WindForceMath.WindForce nearby = cappedFromComponents(
                committed.forceX() + adjustmentX,
                committed.forceZ() + adjustmentZ,
                maximumIntensity
        );
        return new Snapshot(smoothed, adjustment, nearby);
    }

    void reset() {
        smoothedX = 0.0F;
        smoothedZ = 0.0F;
        initialized = false;
    }

    private static WindForceMath.WindForce cappedFromComponents(
            float x, float z, float maximumIntensity
    ) {
        WindForceMath.WindForce force = WindForceMath.fromComponents(x, z);
        if (!force.isPresent()) {
            return WindForceMath.WindForce.NONE;
        }
        if (!Float.isFinite(maximumIntensity) || maximumIntensity <= 0.0F) {
            return WindForceMath.WindForce.NONE;
        }
        if (force.intensity() <= maximumIntensity) {
            return force;
        }
        return new WindForceMath.WindForce(force.x(), force.z(), maximumIntensity);
    }

    private static float magnitude(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }

    record Snapshot(WindForceMath.WindForce smoothed,
                    WindForceMath.WindForce adjustment,
                    WindForceMath.WindForce nearby) {
    }
}
