package com.atmossway.client;

import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.api.behavior.contributors.MultiBlockContributor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

final class WindVariationAnchorResolver {
    private WindVariationAnchorResolver() {
    }

    static BlockPos resolve(BehaviorPipeline pipeline, BlockState state, BlockPos pos) {
        for (MultiBlockContributor contributor : pipeline.getMultiBlockContributors()) {
            if (!contributor.appliesTo(state)) {
                continue;
            }
            BlockPos anchor = contributor.getAnchorPosition(pos, state);
            return anchor == null ? pos : anchor;
        }
        return pos;
    }
}
