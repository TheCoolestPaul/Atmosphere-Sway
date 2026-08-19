package com.atmossway.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtmosphereWindCacheTest {
    @Test
    void synchronizedWindAlwaysWins() {
        assertEquals(
                AtmosphereWindCache.Source.SERVER_SYNC,
                AtmosphereWindCache.selectSource(true, true)
        );
        assertEquals(
                AtmosphereWindCache.Source.SERVER_SYNC,
                AtmosphereWindCache.selectSource(true, false)
        );
    }

    @Test
    void directApiIsOnlyUsedForIntegratedServer() {
        assertEquals(
                AtmosphereWindCache.Source.INTEGRATED_DIRECT,
                AtmosphereWindCache.selectSource(false, true)
        );
        assertEquals(
                AtmosphereWindCache.Source.UNAVAILABLE,
                AtmosphereWindCache.selectSource(false, false)
        );
    }
}
