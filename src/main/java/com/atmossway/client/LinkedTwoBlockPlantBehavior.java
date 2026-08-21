package com.atmossway.client;

import com.github.razorplay01.sway.api.behavior.contributors.DeformationContributor;
import com.github.razorplay01.sway.api.behavior.contributors.MultiBlockContributor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;

final class LinkedTwoBlockPlantBehavior implements MultiBlockContributor, DeformationContributor {
    private final Block lowerBlock;
    private final Block upperBlock;

    LinkedTwoBlockPlantBehavior(Block lowerBlock, Block upperBlock) {
        this.lowerBlock = lowerBlock;
        this.upperBlock = upperBlock;
    }

    @Override
    public boolean appliesTo(BlockState state) {
        return state.is(lowerBlock) || state.is(upperBlock);
    }

    @Override
    public BlockPos getAnchorPosition(BlockPos currentPos, BlockState state) {
        return resolveAnchor(currentPos, state.is(upperBlock));
    }

    @Override
    public Collection<BlockPos> getLinkedBlocks(BlockPos anchorPos, BlockState state, ClientLevel level) {
        return linkedPositions(
                anchorPos,
                level.getBlockState(anchorPos).is(lowerBlock),
                level.getBlockState(anchorPos.above()).is(upperBlock)
        );
    }

    static BlockPos resolveAnchor(BlockPos currentPos, boolean upper) {
        return upper ? currentPos.below() : currentPos;
    }

    static Collection<BlockPos> linkedPositions(BlockPos anchorPos, boolean lowerAtAnchor, boolean upperAbove) {
        if (lowerAtAnchor && upperAbove) {
            return List.of(anchorPos.above());
        }
        return List.of();
    }

    @Override
    public float getVertexWeight(float vertexY, BlockState state, BlockPos pos) {
        return vertexWeight(vertexY, state.is(upperBlock));
    }

    static float vertexWeight(float vertexY, boolean upper) {
        float plantHeight = upper ? 1.0F + vertexY : vertexY;
        float progress = plantHeight / 2.0F;
        return progress > 0.05F ? progress * progress : 0.0F;
    }
}
