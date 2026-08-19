package com.atmossway.mixin;

import com.atmossway.client.SwayUpdateThrottle;
import com.github.razorplay01.sway.client.SwayEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SwayEngine.class, remap = false)
abstract class SwayEngineMixin {
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private static void atmossway$limitUpdateToOnePerGameTick(CallbackInfo callback) {
        if (!SwayUpdateThrottle.shouldRun()) {
            callback.cancel();
        }
    }

    @ModifyConstant(method = "update", constant = @Constant(floatValue = 0.016F))
    private static float atmossway$useClientTickDelta(float original) {
        return 0.05F;
    }
}
