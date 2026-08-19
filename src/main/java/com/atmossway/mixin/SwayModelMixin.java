package com.atmossway.mixin;

import com.atmossway.client.SwayWindModelBridge;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SwayModel.class, remap = false)
abstract class SwayModelMixin {
    @Inject(method = "getModelData", at = @At("RETURN"), cancellable = true)
    private void atmossway$addAmbientWind(BlockAndTintGetter level, BlockPos pos,
                                          BlockState state, ModelData modelData,
                                          CallbackInfoReturnable<ModelData> callback) {
        callback.setReturnValue(SwayWindModelBridge.addAmbientWind(
                level, pos, state, callback.getReturnValue()
        ));
    }
}
