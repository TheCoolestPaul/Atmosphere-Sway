package com.atmossway.client;

final class PrecipitationWindMath {
    static final float SMOOTHING_FACTOR = 0.15F;
    static final float FALL_SPEED_MPS = 9.0F;
    static final float MAX_TILT_DEGREES = 85.0F;
    static final float MAX_TILT_RADIANS = (float) Math.toRadians(MAX_TILT_DEGREES);
    static final float MAX_NATIVE_OFFSET_BLOCKS = 2.0F;
    static final float NATIVE_HALF_RESPONSE_SPEED_MPS = 6.0F;
    private static final float CALM_EPSILON = 0.0001F;

    private PrecipitationWindMath() {
    }

    static WindVector fromAtmosphere(float speedMps, float directionDegrees) {
        if (!Float.isFinite(speedMps) || !Float.isFinite(directionDegrees) || speedMps < 0.0F) {
            return WindVector.INVALID;
        }
        float radians = (float) Math.toRadians(directionDegrees);
        return new WindVector(
                (float) -Math.sin(radians) * speedMps,
                (float) Math.cos(radians) * speedMps,
                true
        );
    }

    static float smooth(float current, float target) {
        return current + (target - current) * SMOOTHING_FACTOR;
    }

    static float interpolate(float previous, float current, float partialTick) {
        float clampedPartialTick = Math.max(0.0F, Math.min(partialTick, 1.0F));
        return previous + (current - previous) * clampedPartialTick;
    }

    static Pose fromVelocity(float velocityX, float velocityZ, float fallbackHeadingRadians) {
        if (!Float.isFinite(velocityX) || !Float.isFinite(velocityZ)) {
            return new Pose(0.0F, fallbackHeadingRadians, 0.0F);
        }
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        float heading = speed > CALM_EPSILON
                ? (float) Math.atan2(-velocityX, velocityZ)
                : fallbackHeadingRadians;
        float tilt = Math.min((float) Math.atan(speed / FALL_SPEED_MPS), MAX_TILT_RADIANS);
        return new Pose(speed, heading, tilt);
    }

    static NativeOffset nativeOffset(Pose pose) {
        if (pose == null || !Float.isFinite(pose.speedMps())
                || !Float.isFinite(pose.headingRadians()) || pose.speedMps() <= CALM_EPSILON) {
            return NativeOffset.NONE;
        }
        float magnitude = MAX_NATIVE_OFFSET_BLOCKS * pose.speedMps()
                / (pose.speedMps() + NATIVE_HALF_RESPONSE_SPEED_MPS);
        // The column base is the later, downwind point. Its top therefore sits upwind.
        float topOffsetX = (float) Math.sin(pose.headingRadians()) * magnitude;
        float topOffsetZ = (float) -Math.cos(pose.headingRadians()) * magnitude;
        return new NativeOffset(topOffsetX, topOffsetZ, magnitude);
    }

    record WindVector(float x, float z, boolean valid) {
        private static final WindVector INVALID = new WindVector(0.0F, 0.0F, false);
    }

    record Pose(float speedMps, float headingRadians, float tiltRadians) {
    }

    record NativeOffset(float x, float z, float magnitude) {
        private static final NativeOffset NONE = new NativeOffset(0.0F, 0.0F, 0.0F);
    }
}
