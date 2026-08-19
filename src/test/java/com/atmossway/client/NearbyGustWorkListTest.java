package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyGustWorkListTest {
    @Test
    void ordersUpdatesNearestFirstWithDeterministicTies() {
        NearbyGustWorkList work = new NearbyGustWorkList();
        work.add(30L, 4, false);
        work.add(20L, 1, false);
        work.add(10L, 4, false);

        assertEquals(20L, work.sectionAt(0));
        assertEquals(10L, work.sectionAt(1));
        assertEquals(30L, work.sectionAt(2));
    }

    @Test
    void placesRestorationWorkAfterNearbyUpdates() {
        NearbyGustWorkList work = new NearbyGustWorkList();
        work.add(5L, 1, true);
        work.add(9L, 9, false);

        assertEquals(9L, work.sectionAt(0));
        assertFalse(work.restorationAt(0));
        assertEquals(5L, work.sectionAt(1));
        assertTrue(work.restorationAt(1));
    }

    @Test
    void resetReusesTheListAndClearsItsLogicalSize() {
        NearbyGustWorkList work = new NearbyGustWorkList();
        for (int index = 0; index < 100; index++) {
            work.add(index, index, false);
        }
        assertEquals(100, work.size());

        work.reset();

        assertEquals(0, work.size());
    }
}
