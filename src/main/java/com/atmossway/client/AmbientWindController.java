package com.atmossway.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AmbientWindController {
    private static final Set<Long> SWAY_SECTIONS = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean TRACKING_CHANGED = new AtomicBoolean();
    private static final NearestSectionSelector ANIMATION_SECTIONS = new NearestSectionSelector();
    private static final SustainedWindLatch WIND_LATCH = new SustainedWindLatch();
    private static final WindAnimationScheduler ANIMATION_SCHEDULER = new WindAnimationScheduler();

    private static volatile ClientLevel activeLevel;
    private static WindForceMath.WindForce currentWind = WindForceMath.WindForce.NONE;
    private static long animationPoseTick;
    private static boolean renderedEnabled;
    private static boolean renderedSwayEnabled;
    private static boolean animationSectionsDirty;
    private static int playerSectionX = Integer.MIN_VALUE;
    private static int playerSectionY = Integer.MIN_VALUE;
    private static int playerSectionZ = Integer.MIN_VALUE;
    private static long lastAnimationListTick = Long.MIN_VALUE;

    private AmbientWindController() {
    }

    public static void track(ClientLevel level, BlockPos pos) {
        if (level == activeLevel) {
            long section = SectionPos.asLong(pos);
            boolean newlyTracked = SWAY_SECTIONS.add(section);
            AtmosSwayDiagnostics.sectionTracked(newlyTracked);
            if (newlyTracked) {
                TRACKING_CHANGED.set(true);
            }
        }
    }

    static void onLevelLoad(ClientLevel level) {
        reset();
        activeLevel = level;
        AtmosSwayDiagnostics.levelLoaded(level);
    }

    static void onLevelUnload(ClientLevel level) {
        if (activeLevel == level) {
            AtmosSwayDiagnostics.levelUnloaded(level);
            reset();
        }
    }

    static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            if (activeLevel != null) {
                reset();
            }
            return;
        }
        if (activeLevel != level) {
            onLevelLoad(level);
        }

        boolean enabled = com.atmossway.config.AtmosSwayConfig.ENABLED.get();
        if (!enabled) {
            PrecipitationWindController.disable();
            if (renderedEnabled) {
                WIND_LATCH.reset();
                AtmosphereWindCache.reset();
                currentWind = WindForceMath.WindForce.NONE;
                AmbientRenderState.publishWind(currentWind);
                renderedEnabled = false;
                AtmosSwayDiagnostics.windCommitted(currentWind, "disabled");
                refreshVisibleSections(minecraft, level, "disabled");
            }
            updateAnimation(minecraft, level, level.getGameTime(), false);
            AtmosSwayDiagnostics.maybeLog(level.getGameTime(), SWAY_SECTIONS.size(), false);
            return;
        }

        AtmosphereWindCache.Sample sample = AtmosphereWindCache.sample(level, minecraft.player);
        PrecipitationWindController.update(
                sample.speedMps(), sample.directionDeg(), sample.valid()
        );
        long gameTick = level.getGameTime();
        boolean swayEnabled = com.github.razorplay01.sway.config.SwayConfig.INSTANCE.enabled;
        if (swayEnabled != renderedSwayEnabled) {
            renderedSwayEnabled = swayEnabled;
            if (!swayEnabled) {
                AmbientRenderState.clearAnimationTargets();
            }
            refreshVisibleSections(minecraft, level,
                    swayEnabled ? "sway_enabled" : "sway_disabled");
        }

        if (!renderedEnabled) {
            renderedEnabled = true;
            if (sample.valid()) {
                WIND_LATCH.initialize(sample.force(), gameTick);
                currentWind = WIND_LATCH.committed();
                AmbientRenderState.publishWind(currentWind);
                AtmosSwayDiagnostics.windCommitted(currentWind, "enabled");
            }
            refreshVisibleSections(minecraft, level, "enabled");
        } else if (sample.regionChanged() && sample.valid()) {
            WIND_LATCH.initialize(sample.force(), gameTick);
            currentWind = WIND_LATCH.committed();
            AmbientRenderState.publishWind(currentWind);
            AtmosSwayDiagnostics.windCommitted(currentWind, "region_changed");
            refreshVisibleSections(minecraft, level, "region_changed");
        } else if (!WIND_LATCH.initialized() && sample.valid()) {
            WIND_LATCH.initialize(sample.force(), gameTick);
            currentWind = WIND_LATCH.committed();
            AmbientRenderState.publishWind(currentWind);
            AtmosSwayDiagnostics.windCommitted(currentWind, "first_valid_sample");
            refreshVisibleSections(minecraft, level, "first_valid_sample");
        } else {
            SustainedWindLatch.WindowResult result = WIND_LATCH.accept(
                    gameTick,
                    sample.force(),
                    sample.valid(),
                    com.atmossway.config.AtmosSwayConfig.STABILIZATION_WINDOW_TICKS.get(),
                    com.atmossway.config.AtmosSwayConfig.RENDER_CHANGE_THRESHOLD.get().floatValue()
            );
            if (result.complete()) {
                AtmosSwayDiagnostics.windWindow(result.average(), WIND_LATCH.committed(), result.committed());
            }
            if (result.committed()) {
                currentWind = WIND_LATCH.committed();
                AmbientRenderState.publishWind(currentWind);
                refreshVisibleSections(minecraft, level, "sustained_wind");
            }
        }
        updateAnimation(minecraft, level, gameTick, swayEnabled);
        AtmosSwayDiagnostics.maybeLog(gameTick, SWAY_SECTIONS.size(), true);
    }

    private static void updateAnimation(Minecraft minecraft, ClientLevel level,
                                        long gameTick, boolean enabled) {
        boolean animate = enabled && currentWind.isPresent();
        if (!animate) {
            ANIMATION_SCHEDULER.prepare(gameTick, 0, false);
            AmbientRenderState.clearAnimationTargets();
            AtmosSwayDiagnostics.animationState(animationPoseTick, 0);
            return;
        }

        int currentSectionX = SectionPos.blockToSectionCoord(minecraft.player.getBlockX());
        int currentSectionY = SectionPos.blockToSectionCoord(minecraft.player.getBlockY());
        int currentSectionZ = SectionPos.blockToSectionCoord(minecraft.player.getBlockZ());
        boolean playerMovedSections = currentSectionX != playerSectionX
                || currentSectionY != playerSectionY
                || currentSectionZ != playerSectionZ;
        if (playerMovedSections) {
            playerSectionX = currentSectionX;
            playerSectionY = currentSectionY;
            playerSectionZ = currentSectionZ;
            rebuildAnimationSections(level);
            ANIMATION_SCHEDULER.restart();
            lastAnimationListTick = gameTick;
        } else {
            if (drainNewAnimationSections()) {
                animationSectionsDirty = true;
            }
            boolean refreshDue = gameTick < lastAnimationListTick
                    || lastAnimationListTick == Long.MIN_VALUE
                    || gameTick - lastAnimationListTick
                    >= WindAnimationScheduler.SECTION_LIST_REFRESH_TICKS;
            if ((refreshDue || animationSectionsDirty) && !ANIMATION_SCHEDULER.passActive()) {
                rebuildAnimationSections(level);
                lastAnimationListTick = gameTick;
            }
        }

        int animationSectionCount = ANIMATION_SECTIONS.size();
        int batchCount = ANIMATION_SCHEDULER.prepare(gameTick, animationSectionCount, true);
        if (ANIMATION_SCHEDULER.passStartedThisTick()) {
            animationPoseTick = ANIMATION_SCHEDULER.poseTick();
            AtmosSwayDiagnostics.animationPassStarted(animationPoseTick, animationSectionCount);
        }

        int invalidated = 0;
        int start = ANIMATION_SCHEDULER.batchStart();
        for (int offset = 0; offset < batchCount; offset++) {
            long packed = ANIMATION_SECTIONS.get(start + offset);
            SectionPos section = SectionPos.of(packed);
            if (!isAnimationSectionEligible(level, section)) {
                animationSectionsDirty = true;
                continue;
            }
            AmbientRenderState.targetAnimation(packed, animationPoseTick);
            minecraft.levelRenderer.setSectionDirty(section.x(), section.y(), section.z());
            invalidated++;
        }
        AtmosSwayDiagnostics.animationSectionsInvalidated(invalidated);
        if (ANIMATION_SCHEDULER.passCompletedThisTick()) {
            AtmosSwayDiagnostics.animationPassCompleted();
        }
        AtmosSwayDiagnostics.animationState(animationPoseTick, animationSectionCount);
    }

    private static void rebuildAnimationSections(ClientLevel level) {
        ANIMATION_SECTIONS.reset(playerSectionX, playerSectionY, playerSectionZ);
        for (long packed : SWAY_SECTIONS) {
            SectionPos section = SectionPos.of(packed);
            if (!level.hasChunk(section.x(), section.z())) {
                SWAY_SECTIONS.remove(packed);
                continue;
            }
            if (isAnimationSectionEligible(level, section)) {
                ANIMATION_SECTIONS.consider(packed, section.x(), section.y(), section.z());
            }
        }
        drainNewAnimationSections();
        AmbientRenderState.retainAnimationTargets(ANIMATION_SECTIONS);
        animationSectionsDirty = false;
    }

    private static boolean drainNewAnimationSections() {
        return TRACKING_CHANGED.getAndSet(false);
    }

    private static boolean isAnimationSectionEligible(ClientLevel level, SectionPos section) {
        return WindAnimationScheduler.isWithinRadius(
                section.x(), section.y(), section.z(),
                playerSectionX, playerSectionY, playerSectionZ
        ) && level.hasChunk(section.x(), section.z());
    }

    private static void refreshVisibleSections(Minecraft minecraft, ClientLevel level, String reason) {
        int playerSectionX = SectionPos.blockToSectionCoord(minecraft.player.getBlockX());
        int playerSectionZ = SectionPos.blockToSectionCoord(minecraft.player.getBlockZ());
        int renderDistance = minecraft.options.getEffectiveRenderDistance();

        int refreshed = 0;
        int evicted = 0;
        for (long packed : SWAY_SECTIONS) {
            SectionPos section = SectionPos.of(packed);
            boolean inView = Math.abs(section.x() - playerSectionX) <= renderDistance
                    && Math.abs(section.z() - playerSectionZ) <= renderDistance;
            boolean loaded = level.hasChunk(section.x(), section.z());
            if (!inView || !loaded) {
                if (SWAY_SECTIONS.remove(packed)) {
                    evicted++;
                }
                continue;
            }

            minecraft.levelRenderer.setSectionDirty(section.x(), section.y(), section.z());
            refreshed++;
        }
        AtmosSwayDiagnostics.sectionsRefreshed(reason, refreshed, evicted);
    }

    private static void reset() {
        activeLevel = null;
        SWAY_SECTIONS.clear();
        TRACKING_CHANGED.set(false);
        ANIMATION_SECTIONS.reset(0, 0, 0);
        animationSectionsDirty = false;
        playerSectionX = Integer.MIN_VALUE;
        playerSectionY = Integer.MIN_VALUE;
        playerSectionZ = Integer.MIN_VALUE;
        lastAnimationListTick = Long.MIN_VALUE;
        currentWind = WindForceMath.WindForce.NONE;
        animationPoseTick = 0L;
        renderedEnabled = false;
        renderedSwayEnabled = false;
        WIND_LATCH.reset();
        ANIMATION_SCHEDULER.reset();
        AmbientRenderState.reset();
        SwayUpdateThrottle.reset();
        AtmosphereWindCache.reset();
        PrecipitationWindController.reset();
        AtmosSwayDiagnostics.resetState();
    }
}
