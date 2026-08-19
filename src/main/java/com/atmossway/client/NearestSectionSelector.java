package com.atmossway.client;

final class NearestSectionSelector {
    static final int MAX_SECTIONS = 6;

    private final long[] sections = new long[MAX_SECTIONS];
    private final int[] distances = new int[MAX_SECTIONS];
    private int playerSectionX;
    private int playerSectionY;
    private int playerSectionZ;
    private int size;

    void reset(int playerSectionX, int playerSectionY, int playerSectionZ) {
        this.playerSectionX = playerSectionX;
        this.playerSectionY = playerSectionY;
        this.playerSectionZ = playerSectionZ;
        size = 0;
    }

    void consider(long packedSection, int sectionX, int sectionY, int sectionZ) {
        if (!WindAnimationScheduler.isWithinRadius(
                sectionX, sectionY, sectionZ,
                playerSectionX, playerSectionY, playerSectionZ
        )) {
            return;
        }

        int dx = sectionX - playerSectionX;
        int dy = sectionY - playerSectionY;
        int dz = sectionZ - playerSectionZ;
        int distance = dx * dx + dy * dy + dz * dz;
        int insertionPoint = findInsertionPoint(distance, packedSection);
        if (insertionPoint >= MAX_SECTIONS) {
            return;
        }

        int oldSize = size;
        if (size < MAX_SECTIONS) {
            size++;
        }
        int elementsToMove = Math.min(oldSize, MAX_SECTIONS - 1) - insertionPoint;
        if (elementsToMove > 0) {
            System.arraycopy(sections, insertionPoint, sections, insertionPoint + 1, elementsToMove);
            System.arraycopy(distances, insertionPoint, distances, insertionPoint + 1, elementsToMove);
        }
        sections[insertionPoint] = packedSection;
        distances[insertionPoint] = distance;
    }

    int size() {
        return size;
    }

    long get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        return sections[index];
    }

    int distanceSquaredAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        return distances[index];
    }

    private int findInsertionPoint(int distance, long packedSection) {
        int index = 0;
        while (index < size) {
            int distanceComparison = Integer.compare(distance, distances[index]);
            if (distanceComparison < 0
                    || distanceComparison == 0 && Long.compare(packedSection, sections[index]) < 0) {
                break;
            }
            index++;
        }
        return index;
    }
}
