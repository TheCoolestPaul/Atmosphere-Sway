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
    private static final LongAdder SPATIAL_VARIATIONS = new LongAdder();
    private static final LongAdder CONTACT_COMBINATIONS = new LongAdder();
    private static final LongAdder NEW_SECTIONS = new LongAdder();
    private static final LongAdder REFRESHED_SECTIONS = new LongAdder();
    private static final LongAdder EVICTED_SECTIONS = new LongAdder();
    private static final LongAdder WIND_COMMITS = new LongAdder();
    private static final LongAdder SUPPRESSED_WINDOWS = new LongAdder();
    private static final LongAdder ANIMATION_PASSES = new LongAdder();
    private static final LongAdder ANIMATION_INVALIDATIONS = new LongAdder();
    private static final LongAdder ANIMATION_TARGET_UPDATES = new LongAdder();
    private static final LongAdder GUST_PROPAGATIONS = new LongAdder();
    private static final LongAdder GUST_INVALIDATIONS = new LongAdder();
    private static final LongAdder GUST_RESTORATIONS = new LongAdder();
    private static final LongAdder SECTION_BUILD_SNAPSHOTS = new LongAdder();
    private static final LongAdder ANIMATED_BUILD_SNAPSHOTS = new LongAdder();
    private static final LongAdder SWAY_SCANS_EXECUTED = new LongAdder();
    private static final LongAdder SWAY_SCANS_SUPPRESSED = new LongAdder();
    private static final AtomicLong TOTAL_UNRESOLVED = new AtomicLong();
    private static final Set<String> WARNED_KEYS = ConcurrentHashMap.newKeySet();

    private static volatile SampleSnapshot latestSample = SampleSnapshot.NONE;
    private static volatile ForceSnapshot latestAverage = ForceSnapshot.NONE;
    private static volatile ForceSnapshot committedWind = ForceSnapshot.NONE;
    private static volatile ForceSnapshot fastSmoothedWind = ForceSnapshot.NONE;
    private static volatile ForceSnapshot gustAdjustment = ForceSnapshot.NONE;
    private static volatile ForceSnapshot nearbyWind = ForceSnapshot.NONE;
    private static volatile String lastCommitReason = "none";
    private static volatile String lastRefreshReason = "none";
    private static volatile long latestAnimationPoseTick;
    private static volatile long previousAnimationPoseTick = Long.MIN_VALUE;
    private static volatile long latestAnimationPoseStepTicks;
    private static volatile int activeAnimationSections;
    private static volatile boolean gustPropagationActive;
    private static volatile boolean gustPropagationPending;
    private static volatile int gustPropagationRemaining;
    private static volatile boolean precipitationActive;
    private static volatile float precipitationSpeedMps;
    private static volatile float precipitationHeadingDegrees;
    private static volatile float precipitationTiltDegrees;
    private static volatile String precipitationRenderer = "inactive";
    private static volatile float precipitationNativeOffset;
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

    static void spatialVariationApplied() {
        SPATIAL_VARIATIONS.increment();
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
        if (debugEnabled() && !"sustained_wind".equals(reason)) {
            AtmosSway.LOGGER.info(
                    "Wind refresh reason={} refreshedSections={} evictedSections={}",
                    reason, refreshed, evicted
            );
        }
    }

    static void windSample(RegionInstanceKey region, long tick, float speedMps,
                           float directionDeg, WindForceMath.WindForce force, boolean valid) {
        latestSample = new SampleSnapshot(
                String.valueOf(region), tick, speedMps, directionDeg,
                force.forceX(), force.forceZ(), force.intensity(), valid
        );
    }

    static void windWindow(WindForceMath.WindForce average,
                           WindForceMath.WindForce committed, boolean accepted) {
        latestAverage = ForceSnapshot.from(average);
        if (accepted) {
            committedWind = ForceSnapshot.from(committed);
            lastCommitReason = "sustained_wind";
            WIND_COMMITS.increment();
        } else {
            SUPPRESSED_WINDOWS.increment();
        }
    }

    static void windCommitted(WindForceMath.WindForce committed, String reason) {
        committedWind = ForceSnapshot.from(committed);
        lastCommitReason = reason;
        WIND_COMMITS.increment();
    }

    static void animationPassStarted(long poseTick, int activeSections) {
        long previousPoseTick = previousAnimationPoseTick;
        latestAnimationPoseStepTicks = previousPoseTick == Long.MIN_VALUE || poseTick < previousPoseTick
                ? 0L
                : poseTick - previousPoseTick;
        previousAnimationPoseTick = poseTick;
        latestAnimationPoseTick = poseTick;
        activeAnimationSections = activeSections;
    }

    static void animationPassCompleted() {
        ANIMATION_PASSES.increment();
    }

    static void animationSectionsInvalidated(int count) {
        ANIMATION_INVALIDATIONS.add(count);
    }

    static void animationTargetUpdated() {
        ANIMATION_TARGET_UPDATES.increment();
    }

    static void gustState(WindForceMath.WindForce smoothed,
                          WindForceMath.WindForce adjustment,
                          WindForceMath.WindForce nearby) {
        fastSmoothedWind = ForceSnapshot.from(smoothed);
        gustAdjustment = ForceSnapshot.from(adjustment);
        nearbyWind = ForceSnapshot.from(nearby);
    }

    static void gustPropagationStarted(int sectionCount) {
        if (sectionCount > 0) {
            GUST_PROPAGATIONS.increment();
        }
    }

    static void gustSectionsInvalidated(int invalidated, int restored) {
        GUST_INVALIDATIONS.add(invalidated);
        GUST_RESTORATIONS.add(restored);
    }

    static void gustPropagationState(boolean active, boolean pending, int remaining) {
        gustPropagationActive = active;
        gustPropagationPending = pending;
        gustPropagationRemaining = remaining;
    }

    static void sectionBuildSnapshotCaptured(boolean animated) {
        SECTION_BUILD_SNAPSHOTS.increment();
        if (animated) {
            ANIMATED_BUILD_SNAPSHOTS.increment();
        }
    }

    static void swayScan(boolean executed) {
        if (executed) {
            SWAY_SCANS_EXECUTED.increment();
        } else {
            SWAY_SCANS_SUPPRESSED.increment();
        }
    }

    static void animationState(long poseTick, int activeSections) {
        if (activeSections == 0) {
            previousAnimationPoseTick = Long.MIN_VALUE;
            latestAnimationPoseStepTicks = 0L;
        }
        latestAnimationPoseTick = poseTick;
        activeAnimationSections = activeSections;
    }

    static void precipitationState(boolean active, float speedMps,
                                   float headingDegrees, float tiltDegrees) {
        precipitationActive = active;
        precipitationSpeedMps = speedMps;
        precipitationHeadingDegrees = headingDegrees;
        precipitationTiltDegrees = tiltDegrees;
    }

    static void precipitationRenderer(String renderer, float nativeOffset) {
        precipitationRenderer = renderer;
        precipitationNativeOffset = nativeOffset;
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
            ForceSnapshot average = latestAverage;
            ForceSnapshot committed = committedWind;
            ForceSnapshot fast = fastSmoothedWind;
            ForceSnapshot adjustment = gustAdjustment;
            ForceSnapshot nearby = nearbyWind;
            AtmosSway.LOGGER.info(
                    "Wind diagnostics enabled={} sampleTick={} region={} speedMps={} directionDeg={} "
                            + "rawValid={} rawForce=({},{}) rawIntensity={} averageForce=({},{}) "
                            + "averageIntensity={} committedForce=({},{}) committedIntensity={} "
                            + "fastForce=({},{}) fastIntensity={} gustAdjustment=({},{}) "
                            + "gustAdjustmentIntensity={} nearbyForce=({},{}) nearbyIntensity={} "
                            + "windCommits={} suppressedWindows={} lastCommit={} "
                            + "animationPoseTick={} animationPoseStepTicks={} animationSections={} "
                            + "animationPasses={} "
                            + "animationInvalidations={} animationCrossEnvelope={} "
                            + "animationAlongEnvelope={} animationSectionCap={} "
                            + "animationBudgetPerTick={} "
                            + "gustPropagations={} gustInvalidations={} gustRestorations={} "
                            + "gustQueueActive={} gustQueuePending={} gustQueueRemaining={} "
                            + "gustBudgetPerTick={} "
                            + "precipitationActive={} precipitationSpeedMps={} "
                            + "precipitationHeadingDeg={} precipitationTiltDeg={} "
                            + "precipitationRenderer={} precipitationNativeOffset={} "
                            + "swayScansExecuted={} swayScansSuppressed={} "
                            + "sectionBuildSnapshots={} animatedBuildSnapshots={} "
                            + "animationTargetUpdates={} "
                            + "hooks={} directLevels={} renderRegions={} "
                            + "unresolved={} qualifying={} applied={} spatiallyVaried={} "
                            + "contactCombined={} newSections={} "
                            + "trackedSections={} exactInvalidations={} evictedSections={} lastRefresh={}",
                    enabled, sample.tick(), sample.region(), sample.speedMps(), sample.directionDeg(),
                    sample.valid(), sample.forceX(), sample.forceZ(), sample.intensity(),
                    average.forceX(), average.forceZ(), average.intensity(),
                    committed.forceX(), committed.forceZ(), committed.intensity(),
                    fast.forceX(), fast.forceZ(), fast.intensity(),
                    adjustment.forceX(), adjustment.forceZ(), adjustment.intensity(),
                    nearby.forceX(), nearby.forceZ(), nearby.intensity(),
                    WIND_COMMITS.sumThenReset(), SUPPRESSED_WINDOWS.sumThenReset(), lastCommitReason,
                    latestAnimationPoseTick, latestAnimationPoseStepTicks, activeAnimationSections,
                    ANIMATION_PASSES.sumThenReset(), ANIMATION_INVALIDATIONS.sumThenReset(),
                    WindSpatialVariation.crosswindEnvelope(nearby.intensity()),
                    WindSpatialVariation.alongWindEnvelope(nearby.intensity()),
                    NearestSectionSelector.MAX_SECTIONS,
                    WindAnimationScheduler.SECTIONS_PER_TICK,
                    GUST_PROPAGATIONS.sumThenReset(), GUST_INVALIDATIONS.sumThenReset(),
                    GUST_RESTORATIONS.sumThenReset(), gustPropagationActive,
                    gustPropagationPending, gustPropagationRemaining,
                    GustPropagationScheduler.SECTIONS_PER_TICK,
                    precipitationActive, precipitationSpeedMps,
                    precipitationHeadingDegrees, precipitationTiltDegrees,
                    precipitationRenderer, precipitationNativeOffset,
                    SWAY_SCANS_EXECUTED.sumThenReset(), SWAY_SCANS_SUPPRESSED.sumThenReset(),
                    SECTION_BUILD_SNAPSHOTS.sumThenReset(), ANIMATED_BUILD_SNAPSHOTS.sumThenReset(),
                    ANIMATION_TARGET_UPDATES.sumThenReset(),
                    MODEL_HOOKS.sumThenReset(), DIRECT_LEVELS.sumThenReset(),
                    RENDER_REGIONS.sumThenReset(), UNRESOLVED_LEVELS.sumThenReset(),
                    QUALIFYING_BLOCKS.sumThenReset(), WIND_APPLICATIONS.sumThenReset(),
                    SPATIAL_VARIATIONS.sumThenReset(), CONTACT_COMBINATIONS.sumThenReset(),
                    NEW_SECTIONS.sumThenReset(),
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
        SPATIAL_VARIATIONS.reset();
        CONTACT_COMBINATIONS.reset();
        NEW_SECTIONS.reset();
        REFRESHED_SECTIONS.reset();
        EVICTED_SECTIONS.reset();
        WIND_COMMITS.reset();
        SUPPRESSED_WINDOWS.reset();
        ANIMATION_PASSES.reset();
        ANIMATION_INVALIDATIONS.reset();
        ANIMATION_TARGET_UPDATES.reset();
        GUST_PROPAGATIONS.reset();
        GUST_INVALIDATIONS.reset();
        GUST_RESTORATIONS.reset();
        SECTION_BUILD_SNAPSHOTS.reset();
        ANIMATED_BUILD_SNAPSHOTS.reset();
        SWAY_SCANS_EXECUTED.reset();
        SWAY_SCANS_SUPPRESSED.reset();
        TOTAL_UNRESOLVED.set(0L);
        WARNED_KEYS.clear();
        latestSample = SampleSnapshot.NONE;
        latestAverage = ForceSnapshot.NONE;
        committedWind = ForceSnapshot.NONE;
        fastSmoothedWind = ForceSnapshot.NONE;
        gustAdjustment = ForceSnapshot.NONE;
        nearbyWind = ForceSnapshot.NONE;
        lastCommitReason = "none";
        lastRefreshReason = "none";
        latestAnimationPoseTick = 0L;
        previousAnimationPoseTick = Long.MIN_VALUE;
        latestAnimationPoseStepTicks = 0L;
        activeAnimationSections = 0;
        gustPropagationActive = false;
        gustPropagationPending = false;
        gustPropagationRemaining = 0;
        precipitationActive = false;
        precipitationSpeedMps = 0.0F;
        precipitationHeadingDegrees = 0.0F;
        precipitationTiltDegrees = 0.0F;
        precipitationRenderer = "inactive";
        precipitationNativeOffset = 0.0F;
        lastSummaryTick = Long.MIN_VALUE;
    }

    private record SampleSnapshot(String region, long tick, float speedMps, float directionDeg,
                                  float forceX, float forceZ, float intensity, boolean valid) {
        private static final SampleSnapshot NONE = new SampleSnapshot(
                "none", Long.MIN_VALUE, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false
        );
    }

    private record ForceSnapshot(float forceX, float forceZ, float intensity) {
        private static final ForceSnapshot NONE = new ForceSnapshot(0.0F, 0.0F, 0.0F);

        private static ForceSnapshot from(WindForceMath.WindForce force) {
            return new ForceSnapshot(force.forceX(), force.forceZ(), force.intensity());
        }
    }
}
