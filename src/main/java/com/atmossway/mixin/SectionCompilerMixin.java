package com.atmossway.mixin;

import com.atmossway.client.AmbientRenderState;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SectionCompiler.class)
abstract class SectionCompilerMixin {
    @Inject(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At("HEAD"))
    private void atmossway$beginSectionBuild(SectionPos section,
                                              RenderChunkRegion region,
                                              VertexSorting sorting,
                                              SectionBufferBuilderPack buffers,
                                              List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers,
                                              CallbackInfoReturnable<SectionCompiler.Results> callback) {
        AmbientRenderState.beginSectionBuild(section);
    }

    @Inject(method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;", at = @At("RETURN"))
    private void atmossway$endSectionBuild(SectionPos section,
                                            RenderChunkRegion region,
                                            VertexSorting sorting,
                                            SectionBufferBuilderPack buffers,
                                            List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers,
                                            CallbackInfoReturnable<SectionCompiler.Results> callback) {
        AmbientRenderState.endSectionBuild();
    }
}
