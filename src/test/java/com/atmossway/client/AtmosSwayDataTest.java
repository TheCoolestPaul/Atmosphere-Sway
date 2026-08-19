package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AtmosSwayDataTest {
    @Test
    void alreadyInterpolatedRenderDataReturnsItself() {
        AtmosSwayData data = new AtmosSwayData(0.2F, 0.8F, 0.5F, 4815L);

        assertSame(data, data.getInterpolated(8.0F));
        assertSame(data, data.getInterpolated(100.0F));
    }

    @Test
    void retainsThePackedRenderPosition() {
        AtmosSwayData data = new AtmosSwayData(0.2F, 0.8F, 0.5F, 4815L);

        assertEquals(4815L, data.packedRenderPosition());
    }
}
