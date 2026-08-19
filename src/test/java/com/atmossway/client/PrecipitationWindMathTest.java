package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecipitationWindMathTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void convertsCardinalAtmosphereDirections() {
        assertVector(0.0F, 4.0F, PrecipitationWindMath.fromAtmosphere(4.0F, 0.0F));
        assertVector(-4.0F, 0.0F, PrecipitationWindMath.fromAtmosphere(4.0F, 90.0F));
        assertVector(0.0F, -4.0F, PrecipitationWindMath.fromAtmosphere(4.0F, 180.0F));
        assertVector(4.0F, 0.0F, PrecipitationWindMath.fromAtmosphere(4.0F, 270.0F));
    }

    @Test
    void rejectsInvalidSamplesButAcceptsCalm() {
        assertFalse(PrecipitationWindMath.fromAtmosphere(Float.NaN, 0.0F).valid());
        assertFalse(PrecipitationWindMath.fromAtmosphere(1.0F, Float.POSITIVE_INFINITY).valid());
        assertFalse(PrecipitationWindMath.fromAtmosphere(-1.0F, 0.0F).valid());

        var calm = PrecipitationWindMath.fromAtmosphere(0.0F, 215.0F);
        assertTrue(calm.valid());
        assertEquals(0.0F, calm.x(), EPSILON);
        assertEquals(0.0F, calm.z(), EPSILON);
    }

    @Test
    void calculatesPhysicalTiltAndCapsExtremeWind() {
        var calm = PrecipitationWindMath.fromVelocity(0.0F, 0.0F, 1.25F);
        assertEquals(0.0F, calm.tiltRadians(), EPSILON);
        assertEquals(1.25F, calm.headingRadians(), EPSILON);

        var nineMetersPerSecond = PrecipitationWindMath.fromVelocity(0.0F, 9.0F, 0.0F);
        assertEquals(45.0F, (float) Math.toDegrees(nineMetersPerSecond.tiltRadians()), EPSILON);

        var extreme = PrecipitationWindMath.fromVelocity(10_000.0F, 0.0F, 0.0F);
        assertEquals(PrecipitationWindMath.MAX_TILT_DEGREES,
                (float) Math.toDegrees(extreme.tiltRadians()), EPSILON);
    }

    @Test
    void vectorSmoothingCrossesDirectionWrapWithoutReversing() {
        var current = PrecipitationWindMath.fromAtmosphere(10.0F, 359.0F);
        var target = PrecipitationWindMath.fromAtmosphere(10.0F, 1.0F);
        float smoothedX = PrecipitationWindMath.smooth(current.x(), target.x());
        float smoothedZ = PrecipitationWindMath.smooth(current.z(), target.z());
        var pose = PrecipitationWindMath.fromVelocity(smoothedX, smoothedZ, 0.0F);
        float headingDegrees = (float) Math.toDegrees(pose.headingRadians());

        assertTrue(Math.abs(headingDegrees) < 2.0F);
    }

    @Test
    void interpolationClampsPartialTicks() {
        assertEquals(2.0F, PrecipitationWindMath.interpolate(2.0F, 6.0F, -1.0F), EPSILON);
        assertEquals(4.0F, PrecipitationWindMath.interpolate(2.0F, 6.0F, 0.5F), EPSILON);
        assertEquals(6.0F, PrecipitationWindMath.interpolate(2.0F, 6.0F, 2.0F), EPSILON);
    }

    @Test
    void nativeOffsetIsBoundedAndPlacesTheColumnTopUpwind() {
        var northward = PrecipitationWindMath.fromVelocity(0.0F, 6.0F, 0.0F);
        var offset = PrecipitationWindMath.nativeOffset(northward);

        assertEquals(1.0F, offset.magnitude(), EPSILON);
        assertEquals(0.0F, offset.x(), EPSILON);
        assertEquals(-1.0F, offset.z(), EPSILON);

        var extreme = PrecipitationWindMath.nativeOffset(
                PrecipitationWindMath.fromVelocity(-100_000.0F, 0.0F, 0.0F)
        );
        assertTrue(extreme.magnitude() < PrecipitationWindMath.MAX_NATIVE_OFFSET_BLOCKS);
        assertTrue(extreme.magnitude() > 1.999F);
    }

    @Test
    void calmNativePrecipitationRemainsVertical() {
        var offset = PrecipitationWindMath.nativeOffset(
                PrecipitationWindMath.fromVelocity(0.0F, 0.0F, 2.0F)
        );

        assertEquals(0.0F, offset.x(), EPSILON);
        assertEquals(0.0F, offset.z(), EPSILON);
        assertEquals(0.0F, offset.magnitude(), EPSILON);
    }

    private static void assertVector(float expectedX, float expectedZ,
                                     PrecipitationWindMath.WindVector actual) {
        assertTrue(actual.valid());
        assertEquals(expectedX, actual.x(), EPSILON);
        assertEquals(expectedZ, actual.z(), EPSILON);
    }
}
