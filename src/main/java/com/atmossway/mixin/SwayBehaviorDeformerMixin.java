package com.atmossway.mixin;

import com.atmossway.client.SwayWindModelBridge;
import com.github.razorplay01.sway.SwayRenderContext;
import com.github.razorplay01.sway.client.render.SwayBehaviorDeformer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.client.SwayData;
import com.github.razorplay01.sway.client.render.VertexMutator;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = SwayBehaviorDeformer.class, remap = false)
abstract class SwayBehaviorDeformerMixin {
    @Inject(method = "deform", at = @At("HEAD"), cancellable = true)
    private static void atmossway$skipWithoutRenderPosition(VertexMutator mutator,
                                                             SwayData data,
                                                             BlockState state,
                                                             BlockPos pos,
                                                             BehaviorPipeline pipeline,
                                                             CallbackInfo callback) {
        if (pos == null && SwayRenderContext.getCurrentBlockPos() == null) {
            SwayWindModelBridge.warnMissingRenderPosition();
            callback.cancel();
        }
    }

    @ModifyVariable(method = "deform", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static BlockPos atmossway$restoreRenderPosition(BlockPos pos) {
        if (pos != null) {
            return pos;
        }

        BlockPos captured = SwayRenderContext.getCurrentBlockPos();
        return captured;
    }
}
