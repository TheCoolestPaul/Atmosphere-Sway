package com.atmossway.client;

import com.atmossway.AtmosSway;
import com.atmossway.config.AtmosSwayConfig;
import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

final class AtmosSwayDiagnostics {
    private static final long SUMMARY_INTERVAL_TICKS = 100L;
    private static final long UNRESOLVED_WARNING_THRESHOLD = 100L;

    private static final LongAdder MODEL_HOOKS = new LongAdder();
    private static final LongAdder DIRECT_LEVELS = new LongAdder();
    private static final LongAdder RENDER_REGIONS = new LongAdder();
    private static final LongAdder UNRESOLVED_LEVELS = new LongAdder();
    private static final LongAdder QUALIFYING_BLOCKS = new LongAdder();
    private static final LongAdder WIND_APPLICATIONS = new LongAdder();
    private static final LongAdder CONTACT_COMBINATIONS = new LongAdder();
    private static final LongAdder NEW_SECTIONS = new LongAdder();
    private static final LongAdder REFRESHED_SECTIONS = new LongAdder();
    private static final LongAdder EVICTED_SECTIONS = new LongAdder();
    private static final AtomicLong TOTAL_UNRESOLVED = new AtomicLong();
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private static volatile SampleSnapshot latestSample = SampleSnapshot.NONE;
    private static volatile String lastRefreshReason = "none";
    private static long lastSummaryTick = Long.MIN_VALUE;

    private AtmosSwayDiagnostics() {
    }

    static void modelHook() {
        MODEL_HOOKS.increment();
    }

    static void directLevelResolved() {
        DIRECT_LEVELS.increment();
    }

    static void renderRegionResolved() {
        RENDER_REGIONS.increment();
    }

    static void unresolvedLevel(String viewType) {
        UNRESOLVED_LEVELS.increment();
        long failures = TOTAL_UNRESOLVED.incrementAndGet();
        String type = viewType == null ? "null" : viewType;
        warnOnce("view:" + type,
                "Unsupported model block view {}; ambient wind cannot be applied to those models", type);
        if (failures >= UNRESOLVED_WARNING_THRESHOLD) {
            warnOnce("unresolved-threshold",
                    "Model hooks repeatedly failed to resolve a client level ({} failures)", failures);
        }
    }

    static void missingRenderPosition() {
        warnOnce("missing-render-position",
                "SWAY supplied no deformation position and had no captured render position; "
                        + "specialized multiblock deformation may be skipped");
    }

    static void qualifyingBlock() {
        QUALIFYING_BLOCKS.increment();
    }

    static void windApplied(boolean combinedWithContact) {
        WIND_APPLICATIONS.increment();
        if (combinedWithContact) {
            CONTACT_COMBINATIONS.increment();
        }
    }

    static void sectionTracked(boolean newlyTracked) {
        if (newlyTracked) {
            NEW_SECTIONS.increment();
        }
    }

    static void sectionsRefreshed(String reason, int refreshed, int evicted) {
        lastRefreshReason = reason;
        REFRESHED_SECTIONS.add(refreshed);
        EVICTED_SECTIONS.add(evicted);
        if (debugEnabled() && !"wind_delta".equals(reason)) {
            AtmosSway.LOGGER.info(
                    "Wind refresh reason={} refreshedSections={} evictedSections={}",
                    reason, refreshed, evicted
            );
        }
    }

    static void windSample(RegionInstanceKey region, long tick, float speedMps,
                           float directionDeg, WindForceMath.WindForce force) {
        latestSample = new SampleSnapshot(
                String.valueOf(region), tick, speedMps, directionDeg,
                force.x(), force.z(), force.intensity()
        );
    }

    static void levelLoaded(ClientLevel level) {
        if (debugEnabled()) {
            AtmosSway.LOGGER.info("Diagnostics started for client level {}", level.dimension().location());
        }
    }

    static void levelUnloaded(ClientLevel level) {
        if (debugEnabled()) {
            AtmosSway.LOGGER.info("Diagnostics stopped for client level {}", level.dimension().location());
        }
    }

    static void resetState() {
        reset();
    }

    static void maybeLog(long gameTick, int trackedSections, boolean enabled) {
        if (!debugEnabled()) {
            return;
        }
        if (gameTick < lastSummaryTick || lastSummaryTick == Long.MIN_VALUE
                || gameTick - lastSummaryTick >= SUMMARY_INTERVAL_TICKS) {
            lastSummaryTick = gameTick;
            SampleSnapshot sample = latestSample;
            AtmosSway.LOGGER.info(
                    "Wind diagnostics enabled={} sampleTick={} region={} speedMps={} directionDeg={} "
                            + "force=({},{}) intensity={} hooks={} directLevels={} renderRegions={} "
                            + "unresolved={} qualifying={} applied={} contactCombined={} newSections={} "
                            + "trackedSections={} refreshedSections={} evictedSections={} lastRefresh={}",
                    enabled, sample.tick(), sample.region(), sample.speedMps(), sample.directionDeg(),
                    sample.forceX(), sample.forceZ(), sample.intensity(),
                    MODEL_HOOKS.sumThenReset(), DIRECT_LEVELS.sumThenReset(),
                    RENDER_REGIONS.sumThenReset(), UNRESOLVED_LEVELS.sumThenReset(),
                    QUALIFYING_BLOCKS.sumThenReset(), WIND_APPLICATIONS.sumThenReset(),
                    CONTACT_COMBINATIONS.sumThenReset(), NEW_SECTIONS.sumThenReset(),
                    trackedSections, REFRESHED_SECTIONS.sumThenReset(),
                    EVICTED_SECTIONS.sumThenReset(), lastRefreshReason
            );
        }
    }

    private static boolean debugEnabled() {
        return AtmosSwayConfig.DEBUG_LOGGING.get();
    }

    private static void warnOnce(String key, String message, Object... arguments) {
        if (WARNED_KEYS.add(key)) {
            AtmosSway.LOGGER.warn(message, arguments);
        }
    }

    private static void reset() {
        MODEL_HOOKS.reset();
        DIRECT_LEVELS.reset();
        RENDER_REGIONS.reset();
        UNRESOLVED_LEVELS.reset();
        QUALIFYING_BLOCKS.reset();
        WIND_APPLICATIONS.reset();
        CONTACT_COMBINATIONS.reset();
        NEW_SECTIONS.reset();
        REFRESHED_SECTIONS.reset();
        EVICTED_SECTIONS.reset();
        TOTAL_UNRESOLVED.set(0L);
        WARNED_KEYS.clear();
        latestSample = SampleSnapshot.NONE;
        lastRefreshReason = "none";
        lastSummaryTick = Long.MIN_VALUE;
    }

    private record SampleSnapshot(String region, long tick, float speedMps, float directionDeg,
                                  float forceX, float forceZ, float intensity) {
        private static final SampleSnapshot NONE = new SampleSnapshot(
                "none", Long.MIN_VALUE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F
        );
    }
}
