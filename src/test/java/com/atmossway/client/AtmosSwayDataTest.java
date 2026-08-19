package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class AtmosSwayDataTest {
    @Test
    void alreadyInterpolatedRenderDataReturnsItself() {
        AtmosSwayData data = new AtmosSwayData(0.2F, 0.8F, 0.5F);

        assertSame(data, data.getInterpolated(8.0F));
        assertSame(data, data.getInterpolated(100.0F));
    }
}
