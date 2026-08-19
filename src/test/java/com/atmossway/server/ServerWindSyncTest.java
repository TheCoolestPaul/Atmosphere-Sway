package com.atmossway.server;

import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerWindSyncTest {
    @Test
    void schedulesEveryFiveTicksIncludingAfterTickRollback() {
        assertTrue(ServerWindSync.shouldSend(0L));
        assertFalse(ServerWindSync.shouldSend(4L));
        assertTrue(ServerWindSync.shouldSend(5L));
        assertTrue(ServerWindSync.shouldSend(-5L));
    }

    @Test
    void validatesFiniteNonnegativeWind() {
        assertTrue(ServerWindSync.isValid(0.0F, 0.0F));
        assertTrue(ServerWindSync.isValid(12.0F, 359.0F));
        assertFalse(ServerWindSync.isValid(-1.0F, 0.0F));
        assertFalse(ServerWindSync.isValid(Float.NaN, 0.0F));
        assertFalse(ServerWindSync.isValid(1.0F, Float.POSITIVE_INFINITY));
    }

    @Test
    void playersInTheSameDimensionAndRegionShareASampleKey() {
        var first = new ServerWindSync.SampleKey(
                Level.OVERWORLD, new RegionInstanceKey(3, -4, 2000)
        );
        var second = new ServerWindSync.SampleKey(
                Level.OVERWORLD, new RegionInstanceKey(3, -4, 2000)
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
