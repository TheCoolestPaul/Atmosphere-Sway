package com.atmossway.network;

import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientWindSyncStateTest {
    private static final ResourceLocation OVERWORLD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation NETHER =
            ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");
    private static final RegionInstanceKey REGION = new RegionInstanceKey(1, -2, 2000);

    @AfterEach
    void reset() {
        ClientWindSyncState.reset();
    }

    @Test
    void acceptsFreshMatchingSampleIncludingCalm() {
        ClientWindSyncState.accept(payload(100L, 0.0F, true), OVERWORLD, REGION, 50L);

        var selected = ClientWindSyncState.select(OVERWORLD, REGION, 55L);
        assertTrue(selected.available());
        assertEquals(0.0F, selected.payload().speedMps());
        assertEquals(5L, selected.ageTicks());
        assertEquals(1L, selected.receivedCount());
    }

    @Test
    void rejectsWrongDimensionAndRegionAtReceipt() {
        ClientWindSyncState.accept(payload(100L, 4.0F, true), NETHER, REGION, 50L);
        ClientWindSyncState.accept(
                payload(101L, 4.0F, true), OVERWORLD,
                new RegionInstanceKey(9, 9, 2000), 51L
        );

        var selected = ClientWindSyncState.select(OVERWORLD, REGION, 52L);
        assertFalse(selected.available());
        assertEquals(2L, selected.rejectedCount());
    }

    @Test
    void rejectsOutOfOrderAndStaleSamples() {
        ClientWindSyncState.accept(payload(100L, 4.0F, true), OVERWORLD, REGION, 50L);
        ClientWindSyncState.accept(payload(99L, 8.0F, true), OVERWORLD, REGION, 51L);

        var fresh = ClientWindSyncState.select(OVERWORLD, REGION, 52L);
        assertTrue(fresh.available());
        assertEquals(4.0F, fresh.payload().speedMps());
        assertEquals(1L, fresh.rejectedCount());

        var stale = ClientWindSyncState.select(
                OVERWORLD, REGION, 50L + ClientWindSyncState.STALE_AFTER_TICKS + 1L
        );
        assertFalse(stale.available());
        assertEquals("stale", stale.match());
    }

    @Test
    void resetRemovesStoredSampleAndCounters() {
        ClientWindSyncState.accept(payload(100L, 4.0F, true), OVERWORLD, REGION, 50L);
        ClientWindSyncState.reset();

        var selected = ClientWindSyncState.select(OVERWORLD, REGION, 51L);
        assertFalse(selected.available());
        assertEquals(0L, selected.receivedCount());
    }

    private static WindSyncPayload payload(long tick, float speed, boolean valid) {
        return new WindSyncPayload(
                OVERWORLD, REGION.regionX(), REGION.regionZ(), REGION.regionSize(),
                tick, speed, 180.0F, valid
        );
    }
}
