package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleRainWindAdapterTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void convertsCardinalHeadingsAtConfiguredScale() {
        assertWind(0.0F, 0.0F, 0.5F);
        assertWind(90.0F, -0.5F, 0.0F);
        assertWind(180.0F, 0.0F, -0.5F);
        assertWind(270.0F, 0.5F, 0.0F);
    }

    @Test
    void activeCalmWindOverridesWithZeroVelocity() {
        var pose = new PrecipitationWindController.Pose(true, 0.0F, 1.25F, 0.0F);
        var wind = ParticleRainWindAdapter.overrideWind(true, true, pose, 0.05F);

        assertEquals(0.0F, wind.x(), EPSILON);
        assertEquals(0.0F, wind.y(), EPSILON);
        assertEquals(0.0F, wind.z(), EPSILON);
    }

    @Test
    void fallsBackWhenControllerOrEitherToggleIsInactive() {
        var active = new PrecipitationWindController.Pose(true, 0.0F, 0.0F, 5.0F);
        var inactive = new PrecipitationWindController.Pose(false, 0.0F, 0.0F, 0.0F);

        assertNull(ParticleRainWindAdapter.overrideWind(false, true, active, 0.05F));
        assertNull(ParticleRainWindAdapter.overrideWind(true, false, active, 0.05F));
        assertNull(ParticleRainWindAdapter.overrideWind(true, true, inactive, 0.05F));
        assertNull(ParticleRainWindAdapter.overrideWind(true, true, null, 0.05F));
    }

    @Test
    void invalidInputsProduceFiniteZeroVelocity() {
        var wind = ParticleRainWindAdapter.windVector(Float.NaN, Float.POSITIVE_INFINITY, -1.0F);
        var overflow = ParticleRainWindAdapter.windVector(Float.MAX_VALUE, 0.0F, Float.MAX_VALUE);

        assertTrue(Float.isFinite(wind.x()));
        assertTrue(Float.isFinite(wind.y()));
        assertTrue(Float.isFinite(wind.z()));
        assertEquals(0.0F, wind.lengthSquared(), EPSILON);
        assertTrue(Float.isFinite(overflow.x()));
        assertTrue(Float.isFinite(overflow.y()));
        assertTrue(Float.isFinite(overflow.z()));
        assertEquals(0.0F, overflow.lengthSquared(), EPSILON);
    }

    @Test
    void returnsFreshVectorsForParticleRainToMutate() {
        var pose = new PrecipitationWindController.Pose(true, 0.0F, 0.0F, 5.0F);
        var first = ParticleRainWindAdapter.overrideWind(true, true, pose, 0.05F);
        var second = ParticleRainWindAdapter.overrideWind(true, true, pose, 0.05F);

        assertNotSame(first, second);
        first.mul(4.0F);
        assertEquals(0.25F, second.z(), EPSILON);
    }

    private static void assertWind(float headingDegrees, float expectedX, float expectedZ) {
        var wind = ParticleRainWindAdapter.windVector(
                10.0F, (float) Math.toRadians(headingDegrees), 0.05F
        );
        assertEquals(expectedX, wind.x(), EPSILON);
        assertEquals(0.0F, wind.y(), EPSILON);
        assertEquals(expectedZ, wind.z(), EPSILON);
    }
}
