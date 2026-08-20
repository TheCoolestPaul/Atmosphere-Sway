package com.atmossway.mixin;

import com.atmossway.client.ParticleRainWindAdapter;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "pigcart.particlerain.ParticleRain", remap = false)
abstract class ParticleRainWindMixin {
    @Inject(method = "getWind(DDD)Lorg/joml/Vector3f;", at = @At("HEAD"),
            cancellable = true, require = 0)
    private static void atmossway$useAtmosphereWind(double x, double y, double z,
                                                     CallbackInfoReturnable<Vector3f> callback) {
        Vector3f wind = ParticleRainWindAdapter.overrideWind();
        if (wind != null) {
            callback.setReturnValue(wind);
        }
    }
}
