package com.atmossway.client;

final class SustainedWindLatch {
    private WindForceMath.WindForce committed = WindForceMath.WindForce.NONE;
    private double accumulatedX;
    private double accumulatedZ;
    private int accumulatedTicks;
    private long lastAcceptedTick = Long.MIN_VALUE;
    private boolean initialized;

    WindForceMath.WindForce committed() {
        return committed;
    }

    boolean initialized() {
        return initialized;
    }

    void initialize(WindForceMath.WindForce wind, long gameTick) {
        committed = wind;
        initialized = true;
        clearWindow();
        lastAcceptedTick = gameTick;
    }

    WindowResult accept(long gameTick, WindForceMath.WindForce raw,
                        boolean valid, int windowTicks, float changeThreshold) {
        if (!initialized || !valid || gameTick == lastAcceptedTick) {
            return WindowResult.INCOMPLETE;
        }
        if (gameTick < lastAcceptedTick) {
            clearWindow();
        }
        lastAcceptedTick = gameTick;
        accumulatedX += raw.forceX();
        accumulatedZ += raw.forceZ();
        accumulatedTicks++;

        if (accumulatedTicks < windowTicks) {
            return WindowResult.INCOMPLETE;
        }

        WindForceMath.WindForce average = WindForceMath.fromComponents(
                (float) (accumulatedX / accumulatedTicks),
                (float) (accumulatedZ / accumulatedTicks)
        );
        clearWindow();

        boolean changed = WindForceMath.vectorDelta(average, committed) >= changeThreshold;
        if (changed) {
            committed = average;
        }
        return new WindowResult(true, changed, average);
    }

    void reset() {
        committed = WindForceMath.WindForce.NONE;
        initialized = false;
        lastAcceptedTick = Long.MIN_VALUE;
        clearWindow();
    }

    private void clearWindow() {
        accumulatedX = 0.0D;
        accumulatedZ = 0.0D;
        accumulatedTicks = 0;
    }

    record WindowResult(boolean complete, boolean committed,
                        WindForceMath.WindForce average) {
        private static final WindowResult INCOMPLETE = new WindowResult(
                false, false, WindForceMath.WindForce.NONE
        );
    }
}
