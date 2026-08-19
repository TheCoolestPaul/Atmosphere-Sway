package com.atmossway.client;

import java.util.Arrays;

final class NearbyGustWorkList {
    private long[] sections = new long[64];
    private int[] distances = new int[64];
    private boolean[] restorations = new boolean[64];
    private int size;

    void reset() {
        size = 0;
    }

    void add(long packedSection, int distanceSquared, boolean restore) {
        ensureCapacity(size + 1);
        int insertion = size;
        while (insertion > 0 && comesBefore(
                distanceSquared, packedSection, restore,
                distances[insertion - 1], sections[insertion - 1], restorations[insertion - 1]
        )) {
            sections[insertion] = sections[insertion - 1];
            distances[insertion] = distances[insertion - 1];
            restorations[insertion] = restorations[insertion - 1];
            insertion--;
        }
        sections[insertion] = packedSection;
        distances[insertion] = distanceSquared;
        restorations[insertion] = restore;
        size++;
    }

    int size() {
        return size;
    }

    long sectionAt(int index) {
        return sections[index];
    }

    boolean restorationAt(int index) {
        return restorations[index];
    }

    private void ensureCapacity(int required) {
        if (required <= sections.length) {
            return;
        }
        int capacity = Math.max(required, sections.length * 2);
        sections = Arrays.copyOf(sections, capacity);
        distances = Arrays.copyOf(distances, capacity);
        restorations = Arrays.copyOf(restorations, capacity);
    }

    private static boolean comesBefore(int distance, long section, boolean restore,
                                       int otherDistance, long otherSection, boolean otherRestore) {
        if (restore != otherRestore) {
            return !restore;
        }
        int distanceComparison = Integer.compare(distance, otherDistance);
        return distanceComparison < 0
                || distanceComparison == 0 && Long.compare(section, otherSection) < 0;
    }
}
