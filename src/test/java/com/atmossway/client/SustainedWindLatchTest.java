package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SustainedWindLatchTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void initializesImmediatelyAndResetRemovesPublishedWind() {
        SustainedWindLatch latch = new SustainedWindLatch();
        var initial = force(0.2F, -0.1F);

        latch.initialize(initial, 42L);
        assertTrue(latch.initialized());
        assertForce(initial, latch.committed());

        latch.reset();
        assertFalse(latch.initialized());
        assertFalse(latch.committed().isPresent());
    }

    @Test
    void holdsPublishedWindUntilAWindowCompletes() {
        SustainedWindLatch latch = new SustainedWindLatch();
        var initial = force(0.1F, 0.0F);
        var changed = force(0.3F, 0.0F);
        latch.initialize(initial, 0L);

        for (long tick = 1L; tick < 100L; tick++) {
            assertFalse(latch.accept(tick, changed, true, 100, 0.05F).complete());
            assertForce(initial, latch.committed());
        }

        var result = latch.accept(100L, changed, true, 100, 0.05F);
        assertTrue(result.complete());
        assertTrue(result.committed());
        assertForce(changed, latch.committed());
    }

    @Test
    void averagesDirectionsAcrossTheDegreeWraparound() {
        SustainedWindLatch latch = new SustainedWindLatch();
        latch.initialize(WindForceMath.WindForce.NONE, 0L);
        var westOfNorth = WindForceMath.fromAtmosphere(1.0F, 350.0F, 1.0D, 2.0D);
        var eastOfNorth = WindForceMath.fromAtmosphere(1.0F, 10.0F, 1.0D, 2.0D);

        latch.accept(1L, westOfNorth, true, 2, 0.0F);
        var result = latch.accept(2L, eastOfNorth, true, 2, 0.0F);

        assertTrue(result.committed());
        assertEquals(0.0F, result.average().x(), EPSILON);
        assertTrue(result.average().z() > 0.98F);
    }

    @Test
    void ignoresInvalidAndDuplicateTickSamples() {
        SustainedWindLatch latch = new SustainedWindLatch();
        latch.initialize(WindForceMath.WindForce.NONE, 10L);
        var wind = force(0.2F, 0.0F);

        assertFalse(latch.accept(11L, wind, false, 2, 0.0F).complete());
        assertFalse(latch.accept(12L, wind, true, 2, 0.0F).complete());
        assertFalse(latch.accept(12L, wind, true, 2, 0.0F).complete());
        assertTrue(latch.accept(13L, wind, true, 2, 0.0F).complete());
    }

    @Test
    void finiteCalmWindCanReplaceCommittedWind() {
        SustainedWindLatch latch = new SustainedWindLatch();
        latch.initialize(force(0.2F, 0.0F), 0L);

        latch.accept(1L, WindForceMath.WindForce.NONE, true, 2, 0.05F);
        var result = latch.accept(2L, WindForceMath.WindForce.NONE, true, 2, 0.05F);

        assertTrue(result.committed());
        assertFalse(latch.committed().isPresent());
    }

    @Test
    void comparesEveryWindowAgainstLastCommittedWind() {
        SustainedWindLatch latch = new SustainedWindLatch();
        latch.initialize(force(0.10F, 0.0F), 0L);

        latch.accept(1L, force(0.13F, 0.0F), true, 2, 0.05F);
        var suppressed = latch.accept(2L, force(0.13F, 0.0F), true, 2, 0.05F);
        assertTrue(suppressed.complete());
        assertFalse(suppressed.committed());
        assertEquals(0.10F, latch.committed().intensity(), EPSILON);

        latch.accept(3L, force(0.16F, 0.0F), true, 2, 0.05F);
        var accepted = latch.accept(4L, force(0.16F, 0.0F), true, 2, 0.05F);
        assertTrue(accepted.committed());
        assertEquals(0.16F, latch.committed().intensity(), EPSILON);
    }

    private static WindForceMath.WindForce force(float forceX, float forceZ) {
        return WindForceMath.fromComponents(forceX, forceZ);
    }

    private static void assertForce(WindForceMath.WindForce expected,
                                    WindForceMath.WindForce actual) {
        assertEquals(expected.forceX(), actual.forceX(), EPSILON);
        assertEquals(expected.forceZ(), actual.forceZ(), EPSILON);
        assertEquals(expected.intensity(), actual.intensity(), EPSILON);
    }
}
