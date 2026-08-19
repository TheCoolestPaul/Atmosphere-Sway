package com.atmossway.client;

final class NearFieldWindTransition {
    static final int DURATION_TICKS = 20;

    private float correctionX;
    private float correctionZ;
    private float appliedCorrectionX;
    private float appliedCorrectionZ;
    private float progress = 1.0F;
    private long startTick = Long.MIN_VALUE;
    private int remainingTicks;
    private boolean active;

    void begin(long gameTick, WindForceMath.WindForce before,
               WindForceMath.WindForce after) {
        correctionX = before.forceX() - after.forceX();
        correctionZ = before.forceZ() - after.forceZ();
        if (!Float.isFinite(correctionX) || !Float.isFinite(correctionZ)
                || magnitude(correctionX, correctionZ) <= 0.001F) {
            reset();
            return;
        }
        appliedCorrectionX = correctionX;
        appliedCorrectionZ = correctionZ;
        progress = 0.0F;
        startTick = gameTick;
        remainingTicks = DURATION_TICKS;
        active = true;
    }

    WindForceMath.WindForce apply(long gameTick, WindForceMath.WindForce target,
                                  float maximumIntensity) {
        if (!active) {
            return target;
        }
        if (gameTick < startTick) {
            reset();
            return target;
        }

        long elapsedTicks = gameTick - startTick;
        if (elapsedTicks >= DURATION_TICKS) {
            reset();
            return target;
        }

        progress = elapsedTicks / (float) DURATION_TICKS;
        float easedProgress = progress * progress * (3.0F - 2.0F * progress);
        float correctionScale = 1.0F - easedProgress;
        appliedCorrectionX = correctionX * correctionScale;
        appliedCorrectionZ = correctionZ * correctionScale;
        remainingTicks = DURATION_TICKS - (int) elapsedTicks;
        return cappedFromComponents(
                target.forceX() + appliedCorrectionX,
                target.forceZ() + appliedCorrectionZ,
                maximumIntensity
        );
    }

    WindForceMath.WindForce correction() {
        return WindForceMath.fromComponents(appliedCorrectionX, appliedCorrectionZ);
    }

    float progress() {
        return progress;
    }

    int remainingTicks() {
        return remainingTicks;
    }

    boolean active() {
        return active;
    }

    void reset() {
        correctionX = 0.0F;
        correctionZ = 0.0F;
        appliedCorrectionX = 0.0F;
        appliedCorrectionZ = 0.0F;
        progress = 1.0F;
        startTick = Long.MIN_VALUE;
        remainingTicks = 0;
        active = false;
    }

    private static WindForceMath.WindForce cappedFromComponents(
            float x, float z, float maximumIntensity
    ) {
        WindForceMath.WindForce force = WindForceMath.fromComponents(x, z);
        if (!force.isPresent() || !Float.isFinite(maximumIntensity) || maximumIntensity <= 0.0F) {
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
}
