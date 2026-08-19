package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GustPropagationSchedulerTest {
    @Test
    void limitsPropagationToTwoSectionsPerTick() {
        GustPropagationScheduler scheduler = new GustPropagationScheduler();
        scheduler.observe(east(0.10F));
        assertTrue(scheduler.shouldStart(0L));
        scheduler.start(0L, 5);

        assertEquals(2, scheduler.nextBatchSize());
        assertEquals(0, scheduler.batchStart());
        scheduler.advance(2);
        assertEquals(2, scheduler.nextBatchSize());
        assertEquals(2, scheduler.batchStart());
        scheduler.advance(2);
        assertEquals(1, scheduler.nextBatchSize());
        scheduler.advance(1);

        assertFalse(scheduler.active());
        assertEquals(0, scheduler.remaining());
    }

    @Test
    void suppressesSmallChangesAndEnforcesTenTickInterval() {
        GustPropagationScheduler scheduler = new GustPropagationScheduler();
        scheduler.observe(east(0.10F));
        scheduler.start(0L, 0);

        scheduler.observe(east(0.12F));
        assertFalse(scheduler.shouldStart(10L));

        scheduler.observe(east(0.13F));
        assertFalse(scheduler.shouldStart(9L));
        assertTrue(scheduler.shouldStart(10L));
    }

    @Test
    void keepsLatestChangePendingUntilActivePassCompletes() {
        GustPropagationScheduler scheduler = new GustPropagationScheduler();
        scheduler.observe(east(0.10F));
        scheduler.start(0L, 4);
        scheduler.observe(east(0.20F));

        assertTrue(scheduler.pending());
        assertFalse(scheduler.shouldStart(10L));

        scheduler.advance(2);
        scheduler.advance(2);
        assertFalse(scheduler.active());
        assertTrue(scheduler.pending());
        assertTrue(scheduler.shouldStart(10L));
    }

    @Test
    void membershipChangesTriggerAPropagationWithoutWindChange() {
        GustPropagationScheduler scheduler = new GustPropagationScheduler();
        scheduler.membershipChanged();

        assertTrue(scheduler.shouldStart(0L));
    }

    private static WindForceMath.WindForce east(float intensity) {
        return new WindForceMath.WindForce(1.0F, 0.0F, intensity);
    }
}
