package com.atmossway.client;

import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.client.SwayData;
import com.github.razorplay01.sway.client.SwayEngine;
import com.github.razorplay01.sway.config.SwayConfig;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import com.atmossway.mixin.RenderChunkRegionAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class SwayWindModelBridge {
    private SwayWindModelBridge() {
    }

    public static void warnMissingRenderPosition() {
        AtmosSwayDiagnostics.missingRenderPosition();
    }

    public static ModelData addAmbientWind(BlockAndTintGetter blockView, BlockPos pos,
                                           BlockState state, ModelData original) {
        AtmosSwayDiagnostics.modelHook();
        if (state == null || original == null) {
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
        WindForceMath.WindForce wind = AmbientWindController.currentWind();
        if (!wind.isPresent()) {
            return original;
        }

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
        WindForceMath.WindForce combined = WindForceMath.vectorSumCapped(contact, wind, cap);
        AtmosSwayDiagnostics.windApplied(contact.isPresent());
        SwayData result = new SwayData(combined.x(), combined.z(), combined.intensity());

        return original.derive()
                .with(SwayModel.SWAY_DATA, result)
                .build();
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
        return null;
    }
}
