package com.atmossway.mixin;

import com.atmossway.client.PrecipitationWindController;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nonamecrackers2.simpleclouds.client.renderer.rain.PrecipitationQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PrecipitationQuad.class, remap = false)
abstract class SimpleCloudsPrecipitationQuadMixin {
    @Unique
    private boolean atmossway$capturedOriginal;
    @Unique
    private boolean atmossway$overridden;
    @Unique
    private float atmossway$originalXRot;
    @Unique
    private float atmossway$originalYRot;

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract void setXRot(float rotation);

    @Shadow
    public abstract float getYRot();

    @Shadow
    public abstract void setYRot(float rotation);

    @Inject(method = "tick", at = @At("HEAD"))
    private void atmossway$applyTickWind(CallbackInfo callback) {
        atmossway$applyPose(PrecipitationWindController.tickPose());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void atmossway$applyFrameWind(PoseStack poseStack, VertexConsumer consumer,
                                          float partialTick, int packedLight,
                                          double cameraX, double cameraY, double cameraZ,
                                          CallbackInfo callback) {
        atmossway$applyPose(PrecipitationWindController.framePose(partialTick));
    }

    @Unique
    private void atmossway$applyPose(PrecipitationWindController.Pose pose) {
        if (!atmossway$capturedOriginal) {
            atmossway$originalXRot = getXRot();
            atmossway$originalYRot = getYRot();
            atmossway$capturedOriginal = true;
        }

        if (pose.active()) {
            setXRot(pose.tiltRadians());
            setYRot(pose.headingRadians());
            atmossway$overridden = true;
        } else if (atmossway$overridden) {
            setXRot(atmossway$originalXRot);
            setYRot(atmossway$originalYRot);
            atmossway$overridden = false;
        }
    }
}
