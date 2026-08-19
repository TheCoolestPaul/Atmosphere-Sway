package com.atmossway.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearestSectionSelectorTest {
    @Test
    void retainsOnlyTheThreeNearestSectionsWithinRadius() {
        NearestSectionSelector selector = new NearestSectionSelector();
        selector.reset(0, 0, 0);

        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    selector.consider(pack(x, y, z), x, y, z);
                }
            }
        }

        assertEquals(NearestSectionSelector.MAX_SECTIONS, selector.size());
        assertEquals(0, selector.distanceSquaredAt(0));
        assertEquals(1, selector.distanceSquaredAt(selector.size() - 1));
        for (int index = 1; index < selector.size(); index++) {
            assertTrue(selector.distanceSquaredAt(index - 1) <= selector.distanceSquaredAt(index));
        }
    }

    @Test
    void resolvesEqualDistancesByPackedSectionPosition() {
        NearestSectionSelector selector = new NearestSectionSelector();
        selector.reset(0, 0, 0);
        List<Long> tied = new ArrayList<>(List.of(
                41L, -12L, 7L, -90L, 18L, 3L
        ));
        selector.consider(41L, 1, 0, 0);
        selector.consider(-12L, -1, 0, 0);
        selector.consider(7L, 0, 1, 0);
        selector.consider(-90L, 0, -1, 0);
        selector.consider(18L, 0, 0, 1);
        selector.consider(3L, 0, 0, -1);
        tied.sort(Long::compare);

        assertEquals(NearestSectionSelector.MAX_SECTIONS, selector.size());
        for (int index = 0; index < selector.size(); index++) {
            assertEquals(tied.get(index).longValue(), selector.get(index));
        }
    }

    @Test
    void resetDropsPreviouslySelectedSections() {
        NearestSectionSelector selector = new NearestSectionSelector();
        selector.reset(0, 0, 0);
        selector.consider(1L, 0, 0, 0);
        assertEquals(1, selector.size());

        selector.reset(10, 4, -8);

        assertEquals(0, selector.size());
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 3) * 49L) + ((long) (y + 3) * 7L) + z + 3L;
    }
}
