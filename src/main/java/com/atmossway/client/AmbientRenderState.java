package com.atmossway.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes immutable wind state to asynchronous terrain-compilation threads.
 */
public final class AmbientRenderState {
    private static final ConcurrentHashMap<Long, Long> ANIMATION_TARGETS = new ConcurrentHashMap<>();
    private static final ThreadLocal<BuildContext> BUILD_CONTEXT = new ThreadLocal<>();

    private static volatile Snapshot baseSnapshot = Snapshot.NONE;

    private AmbientRenderState() {
    }

    static void publishWind(WindForceMath.WindForce wind) {
        baseSnapshot = new Snapshot(wind, 0L, false);
    }

    static void targetAnimation(long packedSection, long poseTick) {
        ANIMATION_TARGETS.put(packedSection, poseTick);
        AtmosSwayDiagnostics.animationTargetUpdated();
    }

    static void retainAnimationTargets(NearestSectionSelector selected) {
        ANIMATION_TARGETS.keySet().removeIf(section -> !selected.contains(section));
    }

    static void clearAnimationTargets() {
        ANIMATION_TARGETS.clear();
    }

    public static void beginSectionBuild(SectionPos section) {
        beginPackedSectionBuild(section.asLong());
    }

    static void beginPackedSectionBuild(long packedSection) {
        Snapshot base = baseSnapshot;
        Long poseTick = ANIMATION_TARGETS.get(packedSection);
        Snapshot captured = poseTick == null
                ? base
                : new Snapshot(base.wind(), poseTick, true);
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
        Long poseTick = ANIMATION_TARGETS.get(packedSection);
        return poseTick == null ? base : new Snapshot(base.wind(), poseTick, true);
    }

    static void reset() {
        baseSnapshot = Snapshot.NONE;
        ANIMATION_TARGETS.clear();
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
