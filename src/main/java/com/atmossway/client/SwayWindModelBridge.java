package com.atmossway.client;

import com.atmossway.config.AtmosSwayConfig;
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.SwayRenderContext;
import com.github.razorplay01.sway.client.SwayData;
import com.github.razorplay01.sway.client.SwayEngine;
import com.github.razorplay01.sway.config.SwayConfig;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import com.atmossway.mixin.RenderChunkRegionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class SwayWindModelBridge {
    private static final ScopedValueContext<BlockPos> RENDER_POSITION_CONTEXT =
            new ScopedValueContext<>();

    private SwayWindModelBridge() {
    }

    public static void warnMissingRenderPosition() {
        AtmosSwayDiagnostics.missingRenderPosition();
    }

    public static ModelData addAmbientWind(BlockAndTintGetter blockView, BlockPos pos,
                                           BlockState state, ModelData original) {
        AtmosSwayDiagnostics.modelHook();
        if (pos == null || state == null || original == null) {
            return original;
        }
        if (!SwayConfig.INSTANCE.enabled) {
            return original;
        }

        ClientLevel level = resolveClientLevel(blockView);
        if (level == null) {
            AtmosSwayDiagnostics.unresolvedLevel(
                    blockView == null ? null : blockView.getClass().getName()
            );
            return original;
        }

        BehaviorPipeline pipeline = SwayAPI.getBehaviorPipeline(state.getBlock());
        if (!SwayAPI.isInteractive(state.getBlock()) || pipeline.getDeformationContributors().isEmpty()) {
            return original;
        }
        AtmosSwayDiagnostics.qualifyingBlock();

        AmbientWindController.track(level, pos);
        AmbientRenderState.Snapshot renderSnapshot = AmbientRenderState.forBlock(pos);
        WindForceMath.WindForce wind = renderSnapshot.wind();
        if (!wind.isPresent()) {
            return original;
        }
        BlockPos variationAnchor = WindVariationAnchorResolver.resolve(pipeline, state, pos);
        WindForceMath.WindForce variedWind = WindSpatialVariation.apply(
                wind,
                variationAnchor.asLong(),
                AtmosSwayConfig.MAX_WIND_INTENSITY.get().floatValue(),
                renderSnapshot.animationPoseTick(),
                renderSnapshot.animated()
        );
        if (!variedWind.isPresent()) {
            return original;
        }
        AtmosSwayDiagnostics.spatialVariationApplied();

        SwayData contactData = original.get(SwayModel.SWAY_DATA);
        SwayData interpolatedContact = contactData == null
                ? null
                : contactData.getInterpolated(SwayEngine.getSmoothness());
        WindForceMath.WindForce contact = interpolatedContact == null
                ? WindForceMath.WindForce.NONE
                : new WindForceMath.WindForce(
                        interpolatedContact.nx,
                        interpolatedContact.nz,
                        interpolatedContact.intensity
                );
        float cap = Math.max(0.0F, SwayConfig.INSTANCE.intensity * 2.0F);
        WindForceMath.WindForce combined = WindForceMath.vectorSumCapped(contact, variedWind, cap);
        AtmosSwayDiagnostics.windApplied(contact.isPresent());
        SwayData result = new AtmosSwayData(
                combined.x(), combined.z(), combined.intensity(), pos.asLong()
        );

        return original.derive()
                .with(SwayModel.SWAY_DATA, result)
                .build();
    }

    public static void beginModelRender(ModelData modelData) {
        BlockPos previous = SwayRenderContext.getCurrentBlockPos();
        SwayData swayData = modelData == null ? null : modelData.get(SwayModel.SWAY_DATA);
        BlockPos renderPosition = swayData instanceof AtmosSwayData data
                ? BlockPos.of(data.packedRenderPosition())
                : null;
        ScopedValueContext.Transition<BlockPos> transition =
                RENDER_POSITION_CONTEXT.begin(previous, renderPosition);
        if (transition.changed()) {
            SwayRenderContext.setCurrentBlockPos(transition.value());
        }
    }

    public static void endModelRender() {
        ScopedValueContext.Transition<BlockPos> transition =
                RENDER_POSITION_CONTEXT.end(SwayRenderContext.getCurrentBlockPos());
        if (transition.changed()) {
            if (transition.value() == null) {
                SwayRenderContext.clear();
            } else {
                SwayRenderContext.setCurrentBlockPos(transition.value());
            }
        }
    }

    static void resetRenderPositionContext() {
        RENDER_POSITION_CONTEXT.reset();
        SwayRenderContext.clear();
    }

    private static ClientLevel resolveClientLevel(BlockAndTintGetter blockView) {
        if (blockView instanceof ClientLevel level) {
            AtmosSwayDiagnostics.directLevelResolved();
            return level;
        }
        if (blockView instanceof RenderChunkRegion region) {
            if (((RenderChunkRegionAccessor) region).atmossway$getLevel() instanceof ClientLevel level) {
                AtmosSwayDiagnostics.renderRegionResolved();
                return level;
            }
        }
        if (blockView != null) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                AtmosSwayDiagnostics.fallbackLevelResolved();
                return level;
            }
        }
        return null;
    }
}
