package com.atmossway.client;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedTwoBlockPlantBehaviorTest {
    private static final BlockPos LOWER_POS = new BlockPos(4, 20, 7);

    @Test
    void anchorsBothBlocksAtTheLowerPosition() {
        assertEquals(LOWER_POS, LinkedTwoBlockPlantBehavior.resolveAnchor(LOWER_POS, false));
        assertEquals(LOWER_POS, LinkedTwoBlockPlantBehavior.resolveAnchor(LOWER_POS.above(), true));
    }

    @Test
    void linksOnlyAValidLowerAndUpperPair() {
        assertEquals(
                List.of(LOWER_POS.above()),
                LinkedTwoBlockPlantBehavior.linkedPositions(LOWER_POS, true, true)
        );
        assertTrue(LinkedTwoBlockPlantBehavior.linkedPositions(LOWER_POS, true, false).isEmpty());
        assertTrue(LinkedTwoBlockPlantBehavior.linkedPositions(LOWER_POS, false, true).isEmpty());
    }

    @Test
    void deformationIsContinuousAcrossTheBlockBoundary() {
        float lowerTop = LinkedTwoBlockPlantBehavior.vertexWeight(1.0F, false);
        float upperBottom = LinkedTwoBlockPlantBehavior.vertexWeight(0.0F, true);

        assertEquals(0.0F, LinkedTwoBlockPlantBehavior.vertexWeight(0.0F, false));
        assertEquals(lowerTop, upperBottom);
        assertEquals(0.25F, lowerTop);
        assertEquals(1.0F, LinkedTwoBlockPlantBehavior.vertexWeight(1.0F, true));
    }
}
