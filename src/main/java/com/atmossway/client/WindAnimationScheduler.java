package com.atmossway.client;

final class WindAnimationScheduler {
    static final int SECTION_RADIUS = 2;
    static final int SECTIONS_PER_TICK = 3;
    static final int SECTION_LIST_REFRESH_TICKS = 20;

    private int cursor;
    private int batchStart;
    private int batchCount;
    private long poseTick;
    private boolean passActive;
    private boolean passStartedThisTick;
    private boolean passCompletedThisTick;

    int prepare(long gameTick, int sectionCount, boolean animate) {
        batchCount = 0;
        passStartedThisTick = false;
        passCompletedThisTick = false;

        if (!animate || sectionCount <= 0) {
            pause();
            return 0;
        }

        if (!passActive) {
            passActive = true;
            passStartedThisTick = true;
            cursor = 0;
            poseTick = gameTick;
        } else if (cursor >= sectionCount) {
            finishPass();
            return 0;
        }

        batchStart = cursor;
        batchCount = Math.min(SECTIONS_PER_TICK, sectionCount - cursor);
        cursor += batchCount;
        if (cursor >= sectionCount) {
            finishPass();
        }
        return batchCount;
    }

    int batchStart() {
        return batchStart;
    }

    long poseTick() {
        return poseTick;
    }

    boolean passActive() {
        return passActive;
    }

    boolean passStartedThisTick() {
        return passStartedThisTick;
    }

    boolean passCompletedThisTick() {
        return passCompletedThisTick;
    }

    void restart() {
        cursor = 0;
        batchStart = 0;
        batchCount = 0;
        passActive = false;
        passStartedThisTick = false;
        passCompletedThisTick = false;
    }

    void reset() {
        restart();
        poseTick = 0L;
    }

    static boolean isWithinRadius(int sectionX, int sectionY, int sectionZ,
                                  int playerSectionX, int playerSectionY, int playerSectionZ) {
        return Math.abs(sectionX - playerSectionX) <= SECTION_RADIUS
                && Math.abs(sectionY - playerSectionY) <= SECTION_RADIUS
                && Math.abs(sectionZ - playerSectionZ) <= SECTION_RADIUS;
    }

    private void pause() {
        cursor = 0;
        passActive = false;
    }

    private void finishPass() {
        passActive = false;
        passCompletedThisTick = true;
    }
}
