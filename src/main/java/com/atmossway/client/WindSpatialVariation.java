package com.atmossway.client;

final class WindSpatialVariation {
    static final float MAX_ANGLE_DEGREES = 8.0F;
    static final float MAX_INTENSITY_VARIATION = 0.10F;
    static final float MAX_CROSSWIND_ANIMATION_FORCE = 0.055F;
    static final float MAX_ALONG_WIND_ANIMATION_FORCE = 0.025F;
    static final float FULL_ANIMATION_INTENSITY = 0.15F;
    static final int ANIMATION_PERIOD_TICKS = 100;

    private static final long ANGLE_SALT = 0x9E3779B97F4A7C15L;
    private static final long INTENSITY_SALT = 0xD1B54A32D192ED03L;
    private static final long ANIMATION_SALT = 0x94D049BB133111EBL;
    private static final float UNIT_FLOAT_SCALE = 0x1.0p-24F;
    private static final int WAVE_TABLE_SIZE = 256;
    private static final float[] SINE_TABLE = createSineTable();
    private static final float[] ROTATION_SINE_TABLE = createRotationTable(true);
    private static final float[] ROTATION_COSINE_TABLE = createRotationTable(false);

    private WindSpatialVariation() {
    }

    static WindForceMath.WindForce apply(WindForceMath.WindForce wind,
                                         long anchorKey, float maximumIntensity,
                                         long animationPoseTick) {
        return apply(wind, anchorKey, maximumIntensity, animationPoseTick, true);
    }

    static WindForceMath.WindForce apply(WindForceMath.WindForce wind,
                                         long anchorKey, float maximumIntensity,
                                         long animationPoseTick, boolean animate) {
        if (!wind.isPresent() || !Float.isFinite(maximumIntensity)
                || maximumIntensity <= 0.0F) {
            return WindForceMath.WindForce.NONE;
        }

        long positionHash = mix(anchorKey);
        int animationIndex = animate ? animationIndex(positionHash, animationPoseTick) : 0;
        float crosswindWave = animate ? SINE_TABLE[animationIndex] : 0.0F;
        float alongWindWave = animate ? SINE_TABLE[
                (animationIndex + WAVE_TABLE_SIZE / 4) & (WAVE_TABLE_SIZE - 1)
        ] : 0.0F;
        int rotationIndex = (int) (mix(positionHash ^ ANGLE_SALT) & (WAVE_TABLE_SIZE - 1));
        float intensityMultiplier = 1.0F
                + signedUnit(mix(positionHash ^ INTENSITY_SALT)) * MAX_INTENSITY_VARIATION;

        float cosine = ROTATION_COSINE_TABLE[rotationIndex];
        float sine = ROTATION_SINE_TABLE[rotationIndex];
        float directionX = wind.x() * cosine - wind.z() * sine;
        float directionZ = wind.x() * sine + wind.z() * cosine;
        float variedIntensity = wind.intensity() * intensityMultiplier;
        float animationScale = animationScale(wind.intensity());
        float crosswindForce = MAX_CROSSWIND_ANIMATION_FORCE * animationScale * crosswindWave;
        float alongWindForce = MAX_ALONG_WIND_ANIMATION_FORCE * animationScale * alongWindWave;

        float forceX = directionX * (variedIntensity + alongWindForce)
                - directionZ * crosswindForce;
        float forceZ = directionZ * (variedIntensity + alongWindForce)
                + directionX * crosswindForce;
        WindForceMath.WindForce varied = WindForceMath.fromComponents(forceX, forceZ);
        if (!varied.isPresent()) {
            return WindForceMath.WindForce.NONE;
        }
        if (varied.intensity() <= maximumIntensity) {
            return varied;
        }
        return new WindForceMath.WindForce(varied.x(), varied.z(), maximumIntensity);
    }

    static float animationScale(float windIntensity) {
        if (!Float.isFinite(windIntensity) || windIntensity <= 0.0F) {
            return 0.0F;
        }
        float normalized = Math.min(windIntensity / FULL_ANIMATION_INTENSITY, 1.0F);
        return normalized * normalized * (3.0F - 2.0F * normalized);
    }

    static float crosswindEnvelope(float windIntensity) {
        return MAX_CROSSWIND_ANIMATION_FORCE * animationScale(windIntensity);
    }

    static float alongWindEnvelope(float windIntensity) {
        return MAX_ALONG_WIND_ANIMATION_FORCE * animationScale(windIntensity);
    }

    private static int animationIndex(long positionHash, long animationPoseTick) {
        long cycleTick = Math.floorMod(animationPoseTick, ANIMATION_PERIOD_TICKS);
        int timeIndex = (int) (cycleTick * WAVE_TABLE_SIZE / ANIMATION_PERIOD_TICKS);
        int plantPhase = (int) (mix(positionHash ^ ANIMATION_SALT) & (WAVE_TABLE_SIZE - 1));
        return (timeIndex + plantPhase) & (WAVE_TABLE_SIZE - 1);
    }

    private static float[] createSineTable() {
        float[] table = new float[WAVE_TABLE_SIZE];
        for (int index = 0; index < table.length; index++) {
            table[index] = (float) Math.sin(index * Math.PI * 2.0D / table.length);
        }
        return table;
    }

    private static float[] createRotationTable(boolean sine) {
        float[] table = new float[WAVE_TABLE_SIZE];
        for (int index = 0; index < table.length; index++) {
            float unit = index / (float) (table.length - 1);
            float degrees = (unit * 2.0F - 1.0F) * MAX_ANGLE_DEGREES;
            double radians = Math.toRadians(degrees);
            table[index] = (float) (sine ? Math.sin(radians) : Math.cos(radians));
        }
        return table;
    }

    private static float signedUnit(long hash) {
        float unit = (hash >>> 40) * UNIT_FLOAT_SCALE;
        return unit * 2.0F - 1.0F;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
