package com.atmossway.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AmbientWindController {
    private static final float REFRESH_THRESHOLD = 0.01F;
    private static final Set<Long> SWAY_SECTIONS = ConcurrentHashMap.newKeySet();

    private static volatile ClientLevel activeLevel;
    private static volatile WindForceMath.WindForce currentWind = WindForceMath.WindForce.NONE;
    private static WindForceMath.WindForce renderedWind = WindForceMath.WindForce.NONE;
    private static boolean renderedEnabled;

    private AmbientWindController() {
    }

    public static WindForceMath.WindForce currentWind() {
        return currentWind;
    }

    public static void track(ClientLevel level, BlockPos pos) {
        if (level == activeLevel) {
            AtmosSwayDiagnostics.sectionTracked(SWAY_SECTIONS.add(SectionPos.asLong(pos)));
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

        WindForceMath.WindForce sampled = AtmosphereWindCache.sample(level, minecraft.player);
        boolean enabled = com.atmossway.config.AtmosSwayConfig.ENABLED.get();
        currentWind = enabled ? sampled : WindForceMath.WindForce.NONE;

        boolean enabledChanged = enabled != renderedEnabled;
        if (WindForceMath.needsRefresh(
                currentWind, renderedWind, enabled, renderedEnabled, REFRESH_THRESHOLD
        )) {
            String reason = enabledChanged ? (enabled ? "enabled" : "disabled") : "wind_delta";
            refreshVisibleSections(minecraft, level, reason);
            renderedWind = currentWind;
            renderedEnabled = enabled;
        }
        AtmosSwayDiagnostics.maybeLog(level.getGameTime(), SWAY_SECTIONS.size(), enabled);
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

            level.setSectionDirtyWithNeighbors(section.x(), section.y(), section.z());
            refreshed++;
        }
        AtmosSwayDiagnostics.sectionsRefreshed(reason, refreshed, evicted);
    }

    private static void reset() {
        activeLevel = null;
        SWAY_SECTIONS.clear();
        currentWind = WindForceMath.WindForce.NONE;
        renderedWind = WindForceMath.WindForce.NONE;
        renderedEnabled = false;
        AtmosphereWindCache.reset();
        AtmosSwayDiagnostics.resetState();
    }
}
