package com.atmossway.client;

import com.github.razorplay01.sway.api.behavior.BehaviorPipeline;
import com.github.razorplay01.sway.api.behavior.contributors.MultiBlockContributor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

final class WindVariationAnchorResolver {
    private WindVariationAnchorResolver() {
    }

    static BlockPos resolve(BehaviorPipeline pipeline, BlockState state, BlockPos pos) {
        BlockPos anchor = pos;
        for (MultiBlockContributor contributor : pipeline.getMultiBlockContributors()) {
            if (!contributor.appliesTo(state)) {
                continue;
            }
            BlockPos resolved = contributor.getAnchorPosition(anchor, state);
            if (resolved != null) {
                anchor = resolved;
            }
        }
        return anchor;
    }
}
