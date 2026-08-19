package com.atmossway.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SwayUpdateThrottle {
    private static final Gate GATE = new Gate();

    private SwayUpdateThrottle() {
    }

    public static boolean shouldRun() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        long gameTick = level == null ? Long.MIN_VALUE : level.getGameTime();
        boolean enabled = com.github.razorplay01.sway.config.SwayConfig.INSTANCE.enabled;
        boolean run = GATE.shouldRun(level, gameTick, enabled);
        AtmosSwayDiagnostics.swayScan(run);
        return run;
    }

    static void reset() {
        GATE.reset();
    }

    static final class Gate {
        private Object lastLevel;
        private long lastGameTick = Long.MIN_VALUE;
        private boolean lastEnabled;
        private boolean initialized;

        boolean shouldRun(Object level, long gameTick, boolean enabled) {
            boolean changed = !initialized
                    || level != lastLevel
                    || gameTick != lastGameTick
                    || enabled != lastEnabled;
            if (changed) {
                initialized = true;
                lastLevel = level;
                lastGameTick = gameTick;
                lastEnabled = enabled;
            }
            return changed;
        }

        void reset() {
            initialized = false;
            lastLevel = null;
            lastGameTick = Long.MIN_VALUE;
            lastEnabled = false;
        }
    }
}
