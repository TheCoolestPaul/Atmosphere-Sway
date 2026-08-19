package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindAnimationSchedulerTest {
    @Test
    void filtersSectionsWithAThreeDimensionalRadius() {
        assertTrue(WindAnimationScheduler.isWithinRadius(2, -2, 2, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(3, 0, 0, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(0, -3, 0, 0, 0, 0));
        assertFalse(WindAnimationScheduler.isWithinRadius(0, 0, 3, 0, 0, 0));
    }

    @Test
    void limitsEveryTickToThreeSectionsAndHoldsOnePoseForThePass() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        assertEquals(3, scheduler.prepare(100L, 8, true));
        assertEquals(0, scheduler.batchStart());
        assertTrue(scheduler.passStartedThisTick());
        assertEquals(100L, scheduler.poseTick());

        assertEquals(3, scheduler.prepare(101L, 8, true));
        assertEquals(3, scheduler.batchStart());
        assertFalse(scheduler.passStartedThisTick());
        assertEquals(100L, scheduler.poseTick());

        assertEquals(2, scheduler.prepare(102L, 8, true));
        assertEquals(6, scheduler.batchStart());
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(100L, scheduler.poseTick());
    }

    @Test
    void startsTheNextPassOnTheFollowingTick() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        assertEquals(1, scheduler.prepare(20L, 1, true));
        assertTrue(scheduler.passCompletedThisTick());

        assertEquals(1, scheduler.prepare(21L, 1, true));
        assertTrue(scheduler.passStartedThisTick());
        assertEquals(21L, scheduler.poseTick());
    }

    @Test
    void incorporatesSectionsAddedDuringAnActivePass() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        assertEquals(3, scheduler.prepare(0L, 5, true));

        assertEquals(3, scheduler.prepare(1L, 8, true));
        assertEquals(3, scheduler.batchStart());
        assertEquals(2, scheduler.prepare(2L, 8, true));
        assertEquals(6, scheduler.batchStart());
        assertTrue(scheduler.passCompletedThisTick());
    }

    @Test
    void completesSixSectionsInTwoTicks() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        for (long tick = 0L; tick < 2L; tick++) {
            assertEquals(3, scheduler.prepare(tick, 6, true));
            assertEquals(tick * 3, scheduler.batchStart());
            assertEquals(0L, scheduler.poseTick());
        }
        assertTrue(scheduler.passCompletedThisTick());

        assertEquals(3, scheduler.prepare(2L, 6, true));
        assertTrue(scheduler.passStartedThisTick());
        assertEquals(2L, scheduler.poseTick());
    }

    @Test
    void threeOrFewerSectionsReceiveANewPoseEveryTick() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        assertEquals(3, scheduler.prepare(40L, 3, true));
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(40L, scheduler.poseTick());

        assertEquals(3, scheduler.prepare(41L, 3, true));
        assertTrue(scheduler.passStartedThisTick());
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(41L, scheduler.poseTick());
    }

    @Test
    void pausingAndRestartingBeginWithTheCurrentPose() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        scheduler.prepare(10L, 8, true);
        assertEquals(0, scheduler.prepare(11L, 8, false));

        assertEquals(3, scheduler.prepare(50L, 8, true));
        assertTrue(scheduler.passStartedThisTick());
        assertEquals(50L, scheduler.poseTick());

        scheduler.restart();
        assertEquals(3, scheduler.prepare(75L, 8, true));
        assertEquals(0, scheduler.batchStart());
        assertEquals(75L, scheduler.poseTick());
    }
}
