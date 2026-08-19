package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindForceMathTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void convertsAtmosphereCardinalDirections() {
        assertForce(0.0F, 1.0F, WindForceMath.fromAtmosphere(10.0F, 0.0F, 0.1D, 2.0D));
        assertForce(-1.0F, 0.0F, WindForceMath.fromAtmosphere(10.0F, 90.0F, 0.1D, 2.0D));
        assertForce(0.0F, -1.0F, WindForceMath.fromAtmosphere(10.0F, 180.0F, 0.1D, 2.0D));
        assertForce(1.0F, 0.0F, WindForceMath.fromAtmosphere(10.0F, 270.0F, 0.1D, 2.0D));
    }

    @Test
    void scalesAndClampsIntensity() {
        assertEquals(0.8F, WindForceMath.fromAtmosphere(10.0F, 0.0F, 0.08D, 1.5D).intensity(), EPSILON);
        assertEquals(1.5F, WindForceMath.fromAtmosphere(60.0F, 0.0F, 0.08D, 1.5D).intensity(), EPSILON);
    }

    @Test
    void rejectsZeroAndInvalidSamples() {
        assertFalse(WindForceMath.fromAtmosphere(0.0F, 0.0F, 0.08D, 1.5D).isPresent());
        assertFalse(WindForceMath.fromAtmosphere(Float.NaN, 0.0F, 0.08D, 1.5D).isPresent());
        assertFalse(WindForceMath.fromAtmosphere(2.0F, Float.POSITIVE_INFINITY, 0.08D, 1.5D).isPresent());
    }

    @Test
    void vectorAdditionReinforcesTurnsAndCancels() {
        var east = new WindForceMath.WindForce(1.0F, 0.0F, 1.0F);
        var north = new WindForceMath.WindForce(0.0F, -1.0F, 1.0F);

        assertEquals(2.0F, WindForceMath.vectorSum(east, east).intensity(), EPSILON);

        var diagonal = WindForceMath.vectorSum(east, north);
        assertEquals((float) Math.sqrt(2.0D), diagonal.intensity(), EPSILON);
        assertTrue(diagonal.x() > 0.0F && diagonal.z() < 0.0F);

        var west = new WindForceMath.WindForce(-1.0F, 0.0F, 1.0F);
        assertFalse(WindForceMath.vectorSum(east, west).isPresent());
    }

    @Test
    void capsCombinedForceWithoutChangingDirection() {
        var east = new WindForceMath.WindForce(1.0F, 0.0F, 1.5F);
        var north = new WindForceMath.WindForce(0.0F, -1.0F, 1.5F);

        var capped = WindForceMath.vectorSumCapped(east, north, 2.0F);
        assertEquals(2.0F, capped.intensity(), EPSILON);
        assertEquals((float) (1.0D / Math.sqrt(2.0D)), capped.x(), EPSILON);
        assertEquals((float) (-1.0D / Math.sqrt(2.0D)), capped.z(), EPSILON);
        assertFalse(WindForceMath.vectorSumCapped(east, north, 0.0F).isPresent());
    }

    @Test
    void measuresAccumulatedVectorChange() {
        var calm = WindForceMath.WindForce.NONE;
        var smallEast = new WindForceMath.WindForce(1.0F, 0.0F, 0.006F);
        var largerEast = new WindForceMath.WindForce(1.0F, 0.0F, 0.012F);
        var west = new WindForceMath.WindForce(-1.0F, 0.0F, 0.012F);

        assertEquals(0.006F, WindForceMath.vectorDelta(smallEast, calm), EPSILON);
        assertEquals(0.012F, WindForceMath.vectorDelta(largerEast, calm), EPSILON);
        assertEquals(0.024F, WindForceMath.vectorDelta(largerEast, west), EPSILON);
    }

    @Test
    void refreshesAdaptivelyAndOnToggle() {
        var rendered = WindForceMath.WindForce.NONE;
        var belowThreshold = new WindForceMath.WindForce(1.0F, 0.0F, 0.009F);
        var atThreshold = new WindForceMath.WindForce(1.0F, 0.0F, 0.01F);

        assertFalse(WindForceMath.needsRefresh(
                belowThreshold, rendered, true, true, 0.01F
        ));
        assertTrue(WindForceMath.needsRefresh(
                atThreshold, rendered, true, true, 0.01F
        ));
        assertTrue(WindForceMath.needsRefresh(
                rendered, rendered, false, true, 0.01F
        ));
    }

    private static void assertForce(float expectedX, float expectedZ, WindForceMath.WindForce actual) {
        assertEquals(expectedX, actual.x(), EPSILON);
        assertEquals(expectedZ, actual.z(), EPSILON);
        assertEquals(1.0F, actual.intensity(), EPSILON);
    }
}
