package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindSpatialVariationTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void producesStableVariationForTheSameAnchor() {
        var wind = WindForceMath.fromComponents(0.5F, 0.0F);
        long anchor = 0x4A21C53B19L;

        var first = WindSpatialVariation.apply(wind, anchor, 1.5F, 37L);
        var second = WindSpatialVariation.apply(wind, anchor, 1.5F, 37L);

        assertForce(first, second);
    }

    @Test
    void differentAnchorsProduceDifferentVariation() {
        var wind = WindForceMath.fromComponents(0.5F, 0.0F);
        var first = WindSpatialVariation.apply(wind, 0x10401L, 1.5F, 37L);
        var second = WindSpatialVariation.apply(wind, 0x20401L, 1.5F, 37L);

        assertNotEquals(first.forceX(), second.forceX());
        assertNotEquals(first.forceZ(), second.forceZ());
    }

    @Test
    void keepsForceAndNormalizationWithinBounds() {
        var wind = new WindForceMath.WindForce(1.0F, 0.0F, 0.5F);
        float maximumAnimationForce = (float) Math.hypot(
                WindSpatialVariation.MAX_CROSSWIND_ANIMATION_FORCE,
                WindSpatialVariation.MAX_ALONG_WIND_ANIMATION_FORCE
        );

        for (int coordinate = -100; coordinate <= 100; coordinate++) {
            var varied = WindSpatialVariation.apply(
                    wind, coordinate * 0x9E3779B9L, 1.5F, 37L
            );
            float directionLength = (float) Math.sqrt(
                    varied.x() * varied.x() + varied.z() * varied.z()
            );

            assertTrue(varied.intensity() <= 0.5F
                    * (1.0F + WindSpatialVariation.MAX_INTENSITY_VARIATION)
                    + maximumAnimationForce + EPSILON);
            assertEquals(1.0F, directionLength, EPSILON);
        }
    }

    @Test
    void animationEnvelopeFadesSmoothlyFromCalmToFullStrength() {
        assertEquals(0.0F, WindSpatialVariation.animationScale(0.0F), EPSILON);
        assertEquals(0.5F, WindSpatialVariation.animationScale(0.075F), EPSILON);
        assertEquals(1.0F, WindSpatialVariation.animationScale(0.15F), EPSILON);
        assertEquals(1.0F, WindSpatialVariation.animationScale(1.0F), EPSILON);
        assertEquals(0.0F, WindSpatialVariation.animationScale(Float.NaN), EPSILON);
        assertEquals(0.0275F, WindSpatialVariation.crosswindEnvelope(0.075F), EPSILON);
        assertEquals(0.0125F, WindSpatialVariation.alongWindEnvelope(0.075F), EPSILON);
    }

    @Test
    void handlesZeroWindAndMaximumIntensity() {
        long anchor = 0x85008L;

        assertFalse(WindSpatialVariation.apply(
                WindForceMath.WindForce.NONE, anchor, 1.5F, 37L
        ).isPresent());

        var capped = WindSpatialVariation.apply(
                new WindForceMath.WindForce(1.0F, 0.0F, 1.5F), anchor, 1.5F, 37L
        );
        assertTrue(capped.intensity() <= 1.5F);
    }

    @Test
    void sharedAnchorKeyProducesOneMultiblockVariation() {
        long sharedAnchor = 0x43C04L;
        var wind = WindForceMath.fromComponents(0.4F, 0.1F);
        assertForce(
                WindSpatialVariation.apply(wind, sharedAnchor, 1.5F, 37L),
                WindSpatialVariation.apply(wind, sharedAnchor, 1.5F, 37L)
        );
    }

    @Test
    void variationDoesNotMutateContactForce() {
        var contact = new WindForceMath.WindForce(-1.0F, 0.0F, 0.3F);
        var wind = new WindForceMath.WindForce(0.0F, 1.0F, 0.4F);
        var variedWind = WindSpatialVariation.apply(wind, 0xC4009L, 1.5F, 37L);

        WindForceMath.vectorSumCapped(contact, variedWind, 2.0F);

        assertEquals(-1.0F, contact.x(), EPSILON);
        assertEquals(0.0F, contact.z(), EPSILON);
        assertEquals(0.3F, contact.intensity(), EPSILON);
    }

    @Test
    void animationChangesPoseAndRepeatsEveryHundredTicks() {
        var wind = new WindForceMath.WindForce(1.0F, 0.0F, 0.5F);
        long anchor = 0x7135A9L;

        var start = WindSpatialVariation.apply(wind, anchor, 1.5F, 0L);
        var quarterCycle = WindSpatialVariation.apply(wind, anchor, 1.5F, 25L);
        var repeated = WindSpatialVariation.apply(wind, anchor, 1.5F, 100L);

        assertTrue(WindForceMath.vectorDelta(start, quarterCycle) > 0.001F);
        assertForce(start, repeated);
    }

    @Test
    void additiveWaveIsVisibleAtTheReportedLightWindStrength() {
        var wind = new WindForceMath.WindForce(1.0F, 0.0F, 0.1705F);
        long anchor = 0x7135A9L;
        float largestHalfCycleDelta = 0.0F;

        for (long tick = 0L; tick < 50L; tick++) {
            var first = WindSpatialVariation.apply(wind, anchor, 1.5F, tick);
            var opposite = WindSpatialVariation.apply(wind, anchor, 1.5F, tick + 50L);
            largestHalfCycleDelta = Math.max(
                    largestHalfCycleDelta,
                    WindForceMath.vectorDelta(first, opposite) * 0.5F
            );
        }

        assertTrue(largestHalfCycleDelta >= 0.054F);
        float defaultPipelineTipPixels = largestHalfCycleDelta * 0.45F * 16.0F;
        assertTrue(defaultPipelineTipPixels >= 0.38F);
    }

    private static void assertForce(WindForceMath.WindForce expected,
                                    WindForceMath.WindForce actual) {
        assertEquals(expected.forceX(), actual.forceX(), EPSILON);
        assertEquals(expected.forceZ(), actual.forceZ(), EPSILON);
        assertEquals(expected.intensity(), actual.intensity(), EPSILON);
    }
}
