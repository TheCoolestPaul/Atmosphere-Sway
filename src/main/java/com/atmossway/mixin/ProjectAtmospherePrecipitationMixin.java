package com.atmossway.mixin;

import com.atmossway.client.PrecipitationWindController;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.Gabou.projectatmosphere.clouds.client.render.CustomPrecipitationRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomPrecipitationRenderer.class, remap = false)
abstract class ProjectAtmospherePrecipitationMixin {
    private static final String FINAL_COLUMN_METHOD =
            "addColumnQuad(Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIDDDDDDDFFFIF)V";

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private static void atmossway$captureNativeFrame(ClientLevel level,
                                                      LightTexture lightTexture,
                                                      float partialTick,
                                                      double cameraX,
                                                      double cameraY,
                                                      double cameraZ,
                                                      CallbackInfoReturnable<Boolean> callback) {
        PrecipitationWindController.beginNativeFrame(partialTick);
    }

    @ModifyVariable(method = FINAL_COLUMN_METHOD, at = @At("HEAD"),
            argsOnly = true, ordinal = 5)
    private static double atmossway$applyNativeWindX(double original) {
        PrecipitationWindController.NativePose pose =
                PrecipitationWindController.nativeFramePose();
        return pose.active() ? pose.topOffsetX() : original;
    }

    @ModifyVariable(method = FINAL_COLUMN_METHOD, at = @At("HEAD"),
            argsOnly = true, ordinal = 6)
    private static double atmossway$applyNativeWindZ(double original) {
        PrecipitationWindController.NativePose pose =
                PrecipitationWindController.nativeFramePose();
        return pose.active() ? pose.topOffsetZ() : original;
    }
}
