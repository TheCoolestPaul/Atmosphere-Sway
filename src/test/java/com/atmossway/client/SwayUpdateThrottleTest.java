package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwayUpdateThrottleTest {
    @Test
    void runsOnlyOnceForRepeatedFramesInOneGameTick() {
        SwayUpdateThrottle.Gate gate = new SwayUpdateThrottle.Gate();
        Object level = new Object();

        assertTrue(gate.shouldRun(level, 100L, true));
        assertFalse(gate.shouldRun(level, 100L, true));
        assertTrue(gate.shouldRun(level, 101L, true));
    }

    @Test
    void runsImmediatelyForWorldTimeAndEnabledTransitions() {
        SwayUpdateThrottle.Gate gate = new SwayUpdateThrottle.Gate();
        Object firstLevel = new Object();
        Object secondLevel = new Object();

        assertTrue(gate.shouldRun(firstLevel, 50L, true));
        assertTrue(gate.shouldRun(firstLevel, 49L, true));
        assertTrue(gate.shouldRun(firstLevel, 49L, false));
        assertTrue(gate.shouldRun(secondLevel, 49L, false));
        assertFalse(gate.shouldRun(secondLevel, 49L, false));
    }

    @Test
    void resetAllowsTheCurrentTickAgain() {
        SwayUpdateThrottle.Gate gate = new SwayUpdateThrottle.Gate();
        Object level = new Object();
        assertTrue(gate.shouldRun(level, 1L, true));
        assertFalse(gate.shouldRun(level, 1L, true));

        gate.reset();

        assertTrue(gate.shouldRun(level, 1L, true));
    }
}
