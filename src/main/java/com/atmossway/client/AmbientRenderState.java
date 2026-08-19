package com.atmossway.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes immutable wind state to asynchronous terrain-compilation threads.
 */
public final class AmbientRenderState {
    private static final ConcurrentHashMap<Long, Snapshot> SECTION_TARGETS = new ConcurrentHashMap<>();
    private static final ThreadLocal<BuildContext> BUILD_CONTEXT = new ThreadLocal<>();

    private static volatile Snapshot baseSnapshot = Snapshot.NONE;

    private AmbientRenderState() {
    }

    static void publishWind(WindForceMath.WindForce wind) {
        baseSnapshot = new Snapshot(wind, 0L, false);
    }

    static void targetAnimation(long packedSection, WindForceMath.WindForce wind, long poseTick) {
        SECTION_TARGETS.put(packedSection, new Snapshot(wind, poseTick, true));
        AtmosSwayDiagnostics.animationTargetUpdated();
    }

    static void targetGust(long packedSection, WindForceMath.WindForce wind) {
        SECTION_TARGETS.put(packedSection, new Snapshot(wind, 0L, false));
    }

    static void clearSectionTarget(long packedSection) {
        SECTION_TARGETS.remove(packedSection);
    }

    static void clearSectionTargets() {
        SECTION_TARGETS.clear();
    }

    public static void beginSectionBuild(SectionPos section) {
        beginPackedSectionBuild(section.asLong());
    }

    static void beginPackedSectionBuild(long packedSection) {
        Snapshot base = baseSnapshot;
        Snapshot target = SECTION_TARGETS.get(packedSection);
        Snapshot captured = target == null ? base : target;
        BUILD_CONTEXT.set(new BuildContext(packedSection, captured));
        AtmosSwayDiagnostics.sectionBuildSnapshotCaptured(captured.animated());
    }

    public static void endSectionBuild() {
        BUILD_CONTEXT.remove();
    }

    static Snapshot forBlock(BlockPos pos) {
        return forPackedSection(SectionPos.asLong(pos));
    }

    static Snapshot forPackedSection(long packedSection) {
        BuildContext context = BUILD_CONTEXT.get();
        if (context != null && context.packedSection() == packedSection) {
            return context.snapshot();
        }
        return baseSnapshot;
    }

    static Snapshot captureForSection(long packedSection) {
        Snapshot base = baseSnapshot;
        Snapshot target = SECTION_TARGETS.get(packedSection);
        return target == null ? base : target;
    }

    static void reset() {
        baseSnapshot = Snapshot.NONE;
        SECTION_TARGETS.clear();
        BUILD_CONTEXT.remove();
    }

    record Snapshot(WindForceMath.WindForce wind, long animationPoseTick, boolean animated) {
        private static final Snapshot NONE = new Snapshot(
                WindForceMath.WindForce.NONE, 0L, false
        );
    }

    private record BuildContext(long packedSection, Snapshot snapshot) {
    }
}
