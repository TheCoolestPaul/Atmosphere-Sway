package com.atmossway.client;

final class WindAnimationScheduler {
    static final int SECTION_RADIUS = 2;
    static final int SECTIONS_PER_TICK = 3;
    static final int SECTION_LIST_REFRESH_TICKS = 20;
    static final int STEADY_POSE_INTERVAL_TICKS = 2;
    static final float URGENT_WIND_DELTA = 0.01F;

    private int cursor;
    private int batchStart;
    private int batchCount;
    private long poseTick;
    private long lastPoseTick = Long.MIN_VALUE;
    private WindForceMath.WindForce lastPoseWind = WindForceMath.WindForce.NONE;
    private boolean passActive;
    private boolean passStartedThisTick;
    private boolean passCompletedThisTick;
    private boolean cadenceSkippedThisTick;
    private PassReason passReason = PassReason.NONE;

    int prepare(long gameTick, int sectionCount, boolean animate,
                WindForceMath.WindForce wind, boolean forceRefresh) {
        batchCount = 0;
        passStartedThisTick = false;
        passCompletedThisTick = false;
        cadenceSkippedThisTick = false;

        if (!animate || sectionCount <= 0) {
            pause();
            return 0;
        }

        if (!passActive) {
            PassReason nextReason = nextPassReason(gameTick, wind, forceRefresh);
            if (nextReason == PassReason.NONE) {
                cadenceSkippedThisTick = true;
                return 0;
            }
            passActive = true;
            passStartedThisTick = true;
            cursor = 0;
            poseTick = gameTick;
            lastPoseTick = gameTick;
            lastPoseWind = wind;
            passReason = nextReason;
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

    boolean cadenceSkippedThisTick() {
        return cadenceSkippedThisTick;
    }

    PassReason passReason() {
        return passReason;
    }

    void restart() {
        cursor = 0;
        batchStart = 0;
        batchCount = 0;
        passActive = false;
        passStartedThisTick = false;
        passCompletedThisTick = false;
        cadenceSkippedThisTick = false;
        passReason = PassReason.NONE;
    }

    void reset() {
        restart();
        poseTick = 0L;
        lastPoseTick = Long.MIN_VALUE;
        lastPoseWind = WindForceMath.WindForce.NONE;
    }

    static boolean isWithinRadius(int sectionX, int sectionY, int sectionZ,
                                  int playerSectionX, int playerSectionY, int playerSectionZ) {
        return Math.abs(sectionX - playerSectionX) <= SECTION_RADIUS
                && Math.abs(sectionY - playerSectionY) <= SECTION_RADIUS
                && Math.abs(sectionZ - playerSectionZ) <= SECTION_RADIUS;
    }

    private void pause() {
        restart();
        lastPoseTick = Long.MIN_VALUE;
        lastPoseWind = WindForceMath.WindForce.NONE;
    }

    private void finishPass() {
        passActive = false;
        passCompletedThisTick = true;
    }

    private PassReason nextPassReason(long gameTick, WindForceMath.WindForce wind,
                                      boolean forceRefresh) {
        if (lastPoseTick == Long.MIN_VALUE || gameTick < lastPoseTick) {
            return PassReason.INITIAL;
        }
        if (forceRefresh) {
            return PassReason.PROXIMITY_CHANGE;
        }
        if (WindForceMath.vectorDelta(lastPoseWind, wind) >= URGENT_WIND_DELTA) {
            return PassReason.WIND_DELTA;
        }
        if (gameTick - lastPoseTick >= STEADY_POSE_INTERVAL_TICKS) {
            return PassReason.INTERVAL;
        }
        return PassReason.NONE;
    }

    enum PassReason {
        NONE("none", false),
        INITIAL("initial", false),
        INTERVAL("interval", false),
        WIND_DELTA("wind_delta", true),
        PROXIMITY_CHANGE("proximity_change", true),
        FINAL_CLEAR("final_clear", true);

        private final String diagnosticName;
        private final boolean urgent;

        PassReason(String diagnosticName, boolean urgent) {
            this.diagnosticName = diagnosticName;
            this.urgent = urgent;
        }

        String diagnosticName() {
            return diagnosticName;
        }

        boolean urgent() {
            return urgent;
        }
    }
}
