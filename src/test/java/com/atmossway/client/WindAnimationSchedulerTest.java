package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindAnimationSchedulerTest {
    private static final WindForceMath.WindForce STEADY_WIND =
            WindForceMath.fromComponents(0.20F, 0.0F);

    @Test
    void filtersSectionsWithAThreeDimensionalRadius() {
        assertTrue(WindAnimationScheduler.isWithinRadius(2, -2, 2, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(3, 0, 0, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(0, -3, 0, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(0, 0, 3, 0, 0, 0));
    }

    @Test
    void steadyWindPublishesOneSynchronizedPoseEveryTwoTicks() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        assertEquals(3, scheduler.prepare(0L, 3, true, STEADY_WIND, false));
        assertEquals(WindAnimationScheduler.PassReason.INITIAL, scheduler.passReason());
        assertEquals(0L, scheduler.poseTick());
        assertTrue(scheduler.passStartedThisTick());
        assertTrue(scheduler.passCompletedThisTick());

        assertEquals(0, scheduler.prepare(1L, 3, true, STEADY_WIND, false));
        assertTrue(scheduler.cadenceSkippedThisTick());

        assertEquals(3, scheduler.prepare(2L, 3, true, STEADY_WIND, false));
        assertEquals(WindAnimationScheduler.PassReason.INTERVAL, scheduler.passReason());
        assertEquals(2L, scheduler.poseTick());

        assertEquals(0, scheduler.prepare(3L, 3, true, STEADY_WIND, false));
        assertEquals(3, scheduler.prepare(4L, 3, true, STEADY_WIND, false));
        assertEquals(4L, scheduler.poseTick());
    }

    @Test
    void materialWindDeltaTriggersAnImmediateOddTickPass() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(10L, 3, true, STEADY_WIND, false);
        var gust = WindForceMath.fromComponents(0.211F, 0.0F);

        assertEquals(3, scheduler.prepare(11L, 3, true, gust, false));
        assertEquals(WindAnimationScheduler.PassReason.WIND_DELTA, scheduler.passReason());
        assertTrue(scheduler.passReason().urgent());
        assertEquals(11L, scheduler.poseTick());
    }

    @Test
    void subThresholdWindChangeWaitsNoMoreThanOneTick() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(20L, 3, true, STEADY_WIND, false);
        var smallChange = WindForceMath.fromComponents(0.205F, 0.0F);

        assertEquals(0, scheduler.prepare(21L, 3, true, smallChange, false));
        assertTrue(scheduler.cadenceSkippedThisTick());
        assertEquals(3, scheduler.prepare(22L, 3, true, smallChange, false));
        assertEquals(WindAnimationScheduler.PassReason.INTERVAL, scheduler.passReason());
    }

    @Test
    void proximityChangeForcesAnImmediateSynchronizedPass() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(30L, 3, true, STEADY_WIND, false);

        assertEquals(3, scheduler.prepare(31L, 3, true, STEADY_WIND, true));
        assertEquals(WindAnimationScheduler.PassReason.PROXIMITY_CHANGE, scheduler.passReason());
        assertTrue(scheduler.passReason().urgent());
        assertEquals(0, scheduler.batchStart());
        assertTrue(scheduler.passCompletedThisTick());
    }

    @Test
    void timeRollbackAndReenablePublishAnInitialPoseImmediately() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(100L, 3, true, STEADY_WIND, false);

        assertEquals(3, scheduler.prepare(99L, 3, true, STEADY_WIND, false));
        assertEquals(WindAnimationScheduler.PassReason.INITIAL, scheduler.passReason());

        assertEquals(0, scheduler.prepare(
                100L, 3, false, WindForceMath.WindForce.NONE, false
        ));
        assertEquals(3, scheduler.prepare(150L, 3, true, STEADY_WIND, false));
        assertEquals(WindAnimationScheduler.PassReason.INITIAL, scheduler.passReason());
    }

    @Test
    void restartAllowsAForcedProximityRefreshWithoutLosingWindHistory() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(40L, 3, true, STEADY_WIND, false);

        scheduler.restart();

        assertEquals(3, scheduler.prepare(41L, 3, true, STEADY_WIND, true));
        assertEquals(WindAnimationScheduler.PassReason.PROXIMITY_CHANGE, scheduler.passReason());
    }

    @Test
    void disabledOrEmptyAnimationDoesNotReportACadenceSkip() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        assertEquals(0, scheduler.prepare(0L, 0, true, STEADY_WIND, false));
        assertFalse(scheduler.cadenceSkippedThisTick());
        assertEquals(0, scheduler.prepare(1L, 3, false, STEADY_WIND, false));
        assertFalse(scheduler.cadenceSkippedThisTick());
    }
}
