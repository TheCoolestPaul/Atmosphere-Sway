package com.atmossway.client;

public final class PrecipitationWindController {
    private static final Pose INACTIVE_POSE = new Pose(false, 0.0F, 0.0F, 0.0F);
    private static volatile Snapshot snapshot = Snapshot.INACTIVE;
    private static volatile NativePose nativeFramePose = NativePose.INACTIVE;

    private static long nextVersion;
    private static long cachedFrameVersion = Long.MIN_VALUE;
    private static int cachedPartialTickBits;
    private static Pose cachedFramePose = INACTIVE_POSE;

    private PrecipitationWindController() {
    }

    static void update(float speedMps, float directionDegrees, boolean valid) {
        if (!valid) {
            holdLastValidSample();
            return;
        }
        PrecipitationWindMath.WindVector target = PrecipitationWindMath.fromAtmosphere(
                speedMps, directionDegrees
        );
        if (!target.valid()) {
            holdLastValidSample();
            return;
        }

        Snapshot old = snapshot;
        float previousX;
        float previousZ;
        float currentX;
        float currentZ;
        float fallbackHeading;
        if (!old.active()) {
            previousX = target.x();
            previousZ = target.z();
            currentX = target.x();
            currentZ = target.z();
            fallbackHeading = (float) Math.toRadians(directionDegrees);
        } else {
            previousX = old.currentX();
            previousZ = old.currentZ();
            currentX = PrecipitationWindMath.smooth(old.currentX(), target.x());
            currentZ = PrecipitationWindMath.smooth(old.currentZ(), target.z());
            fallbackHeading = old.fallbackHeadingRadians();
        }

        PrecipitationWindMath.Pose tickPose = PrecipitationWindMath.fromVelocity(
                currentX, currentZ, fallbackHeading
        );
        if (tickPose.speedMps() > 0.0001F) {
            fallbackHeading = tickPose.headingRadians();
        }
        snapshot = new Snapshot(
                true, previousX, previousZ, currentX, currentZ,
                fallbackHeading, ++nextVersion, Pose.from(tickPose)
        );
        report(tickPose);
    }

    static void disable() {
        snapshot = Snapshot.INACTIVE;
        nativeFramePose = NativePose.INACTIVE;
        invalidateFrameCache();
        AtmosSwayDiagnostics.precipitationState(false, 0.0F, 0.0F, 0.0F);
        AtmosSwayDiagnostics.precipitationRenderer("inactive", 0.0F);
    }

    static void reset() {
        nextVersion = 0L;
        disable();
    }

    public static Pose tickPose() {
        return snapshot.tickPose();
    }

    public static Pose framePose(float partialTick) {
        Snapshot current = snapshot;
        if (!current.active()) {
            return INACTIVE_POSE;
        }
        AtmosSwayDiagnostics.precipitationRenderer("simple_clouds_fallback", 0.0F);
        int partialTickBits = Float.floatToIntBits(partialTick);
        if (cachedFrameVersion == current.version() && cachedPartialTickBits == partialTickBits) {
            return cachedFramePose;
        }

        float velocityX = PrecipitationWindMath.interpolate(
                current.previousX(), current.currentX(), partialTick
        );
        float velocityZ = PrecipitationWindMath.interpolate(
                current.previousZ(), current.currentZ(), partialTick
        );
        Pose pose = Pose.from(PrecipitationWindMath.fromVelocity(
                velocityX, velocityZ, current.fallbackHeadingRadians()
        ));
        cachedFramePose = pose;
        cachedPartialTickBits = partialTickBits;
        cachedFrameVersion = current.version();
        return pose;
    }

    public static void beginNativeFrame(float partialTick) {
        Pose pose = framePose(partialTick);
        if (!pose.active()) {
            nativeFramePose = NativePose.INACTIVE;
            AtmosSwayDiagnostics.precipitationRenderer("inactive", 0.0F);
            return;
        }
        PrecipitationWindMath.NativeOffset offset = PrecipitationWindMath.nativeOffset(
                new PrecipitationWindMath.Pose(
                        pose.speedMps(), pose.headingRadians(), pose.tiltRadians()
                )
        );
        nativeFramePose = new NativePose(true, offset.x(), offset.z(), offset.magnitude());
        AtmosSwayDiagnostics.precipitationRenderer("project_atmosphere", offset.magnitude());
    }

    public static NativePose nativeFramePose() {
        return nativeFramePose;
    }

    private static void holdLastValidSample() {
        Snapshot old = snapshot;
        if (!old.active()) {
            return;
        }
        snapshot = new Snapshot(
                true, old.currentX(), old.currentZ(), old.currentX(), old.currentZ(),
                old.fallbackHeadingRadians(), ++nextVersion, old.tickPose()
        );
        report(new PrecipitationWindMath.Pose(
                old.tickPose().speedMps(), old.tickPose().headingRadians(), old.tickPose().tiltRadians()
        ));
    }

    private static void report(PrecipitationWindMath.Pose pose) {
        AtmosSwayDiagnostics.precipitationState(
                true,
                pose.speedMps(),
                normalizeDegrees((float) Math.toDegrees(pose.headingRadians())),
                (float) Math.toDegrees(pose.tiltRadians())
        );
    }

    private static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    private static void invalidateFrameCache() {
        cachedFrameVersion = Long.MIN_VALUE;
        cachedPartialTickBits = 0;
        cachedFramePose = INACTIVE_POSE;
    }

    public record Pose(boolean active, float tiltRadians, float headingRadians, float speedMps) {
        private static Pose from(PrecipitationWindMath.Pose pose) {
            return new Pose(true, pose.tiltRadians(), pose.headingRadians(), pose.speedMps());
        }
    }

    public record NativePose(boolean active, float topOffsetX,
                             float topOffsetZ, float magnitude) {
        private static final NativePose INACTIVE = new NativePose(false, 0.0F, 0.0F, 0.0F);
    }

    private record Snapshot(boolean active,
                            float previousX, float previousZ,
                            float currentX, float currentZ,
                            float fallbackHeadingRadians,
                            long version, Pose tickPose) {
        private static final Snapshot INACTIVE = new Snapshot(
                false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0L, INACTIVE_POSE
        );
    }
}
