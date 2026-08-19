package com.atmossway.client;

final class GustPropagationScheduler {
    static final int SECTIONS_PER_TICK = 2;
    static final long MINIMUM_INTERVAL_TICKS = 10L;
    static final float CHANGE_THRESHOLD = 0.025F;

    private WindForceMath.WindForce latestTarget = WindForceMath.WindForce.NONE;
    private WindForceMath.WindForce passTarget = WindForceMath.WindForce.NONE;
    private WindForceMath.WindForce lastPropagated = WindForceMath.WindForce.NONE;
    private long lastStartTick = Long.MIN_VALUE;
    private int cursor;
    private int total;
    private boolean active;
    private boolean targetPending;
    private boolean membershipPending;

    void observe(WindForceMath.WindForce target) {
        latestTarget = target;
        WindForceMath.WindForce comparison = active ? passTarget : lastPropagated;
        targetPending = WindForceMath.vectorDelta(target, comparison) >= CHANGE_THRESHOLD;
    }

    void membershipChanged() {
        membershipPending = true;
    }

    boolean shouldStart(long gameTick) {
        if (active || !targetPending && !membershipPending) {
            return false;
        }
        return lastStartTick == Long.MIN_VALUE
                || gameTick < lastStartTick
                || gameTick - lastStartTick >= MINIMUM_INTERVAL_TICKS;
    }

    void start(long gameTick, int sectionCount) {
        passTarget = latestTarget;
        lastStartTick = gameTick;
        cursor = 0;
        total = Math.max(0, sectionCount);
        active = total > 0;
        targetPending = false;
        membershipPending = false;
        if (!active) {
            complete();
        }
    }

    int nextBatchSize() {
        if (!active) {
            return 0;
        }
        return Math.min(SECTIONS_PER_TICK, total - cursor);
    }

    int batchStart() {
        return cursor;
    }

    void advance(int count) {
        if (!active) {
            return;
        }
        cursor += Math.max(0, count);
        if (cursor >= total) {
            complete();
        }
    }

    boolean active() {
        return active;
    }

    int remaining() {
        return active ? total - cursor : 0;
    }

    boolean pending() {
        return targetPending || membershipPending;
    }

    WindForceMath.WindForce passTarget() {
        return passTarget;
    }

    void reset() {
        latestTarget = WindForceMath.WindForce.NONE;
        passTarget = WindForceMath.WindForce.NONE;
        lastPropagated = WindForceMath.WindForce.NONE;
        lastStartTick = Long.MIN_VALUE;
        cursor = 0;
        total = 0;
        active = false;
        targetPending = false;
        membershipPending = false;
    }

    private void complete() {
        lastPropagated = passTarget;
        active = false;
        cursor = total;
        targetPending = WindForceMath.vectorDelta(latestTarget, lastPropagated)
                >= CHANGE_THRESHOLD;
    }
}
