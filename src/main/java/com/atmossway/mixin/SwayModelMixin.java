package com.atmossway.mixin;

import com.atmossway.client.SwayWindModelBridge;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

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

    @Inject(method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;", at = @At("HEAD"))
    private void atmossway$beginModelRender(BlockState state, Direction direction,
                                             RandomSource random, ModelData modelData,
                                             RenderType renderType,
                                             CallbackInfoReturnable<List<BakedQuad>> callback) {
        SwayWindModelBridge.beginModelRender(modelData);
    }

    @Inject(method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;", at = @At("RETURN"))
    private void atmossway$endModelRender(BlockState state, Direction direction,
                                           RandomSource random, ModelData modelData,
                                           RenderType renderType,
                                           CallbackInfoReturnable<List<BakedQuad>> callback) {
        SwayWindModelBridge.endModelRender();
    }
}
