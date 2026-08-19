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
    void allThreeSelectedSectionsReceiveOneSynchronizedPoseEveryTick() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();

        assertEquals(3, scheduler.prepare(100L, 3, true));
        assertEquals(0, scheduler.batchStart());
        assertTrue(scheduler.passStartedThisTick());
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(100L, scheduler.poseTick());

        assertEquals(3, scheduler.prepare(101L, 3, true));
        assertEquals(0, scheduler.batchStart());
        assertTrue(scheduler.passStartedThisTick());
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(101L, scheduler.poseTick());
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
    void fewerThanThreeSectionsStillReceiveANewPoseEveryTick() {
        WindAnimationScheduler scheduler = new WindAnimationScheduler();
        assertEquals(2, scheduler.prepare(0L, 2, true));
        assertTrue(scheduler.passCompletedThisTick());

        assertEquals(2, scheduler.prepare(1L, 2, true));
        assertTrue(scheduler.passStartedThisTick());
        assertTrue(scheduler.passCompletedThisTick());
        assertEquals(1L, scheduler.poseTick());
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
        scheduler.prepare(10L, 3, true);
        assertEquals(0, scheduler.prepare(11L, 3, false));

        assertEquals(3, scheduler.prepare(50L, 3, true));
        assertTrue(scheduler.passStartedThisTick());
        assertEquals(50L, scheduler.poseTick());

        scheduler.restart();
        assertEquals(3, scheduler.prepare(75L, 3, true));
        assertEquals(0, scheduler.batchStart());
        assertEquals(75L, scheduler.poseTick());
    }
}
