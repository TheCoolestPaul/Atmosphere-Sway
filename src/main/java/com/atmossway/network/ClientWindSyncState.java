package com.atmossway.network;

import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicLong;

public final class ClientWindSyncState {
    public static final long STALE_AFTER_TICKS = 40L;

    private static final AtomicLong RECEIVED = new AtomicLong();
    private static final AtomicLong REJECTED = new AtomicLong();
    private static volatile StoredSample latest;

    private ClientWindSyncState() {
    }

    public static void accept(WindSyncPayload payload, ResourceLocation currentDimension,
                              RegionInstanceKey currentRegion, long localTick) {
        if (!isWellFormed(payload)
                || !payload.dimension().equals(currentDimension)
                || !matches(payload, currentRegion)) {
            REJECTED.incrementAndGet();
            return;
        }

        StoredSample previous = latest;
        if (previous != null
                && previous.payload().dimension().equals(payload.dimension())
                && matches(previous.payload(), currentRegion)
                && payload.serverTick() < previous.payload().serverTick()) {
            REJECTED.incrementAndGet();
            return;
        }

        latest = new StoredSample(payload, localTick);
        RECEIVED.incrementAndGet();
    }

    public static Selection select(ResourceLocation dimension, RegionInstanceKey region,
                                   long localTick) {
        StoredSample stored = latest;
        if (stored == null) {
            return Selection.unavailable("missing", Long.MAX_VALUE);
        }
        if (!stored.payload().dimension().equals(dimension)) {
            return Selection.unavailable("dimension_mismatch", age(localTick, stored.localTick()));
        }
        if (!matches(stored.payload(), region)) {
            return Selection.unavailable("region_mismatch", age(localTick, stored.localTick()));
        }
        long age = age(localTick, stored.localTick());
        if (age < 0L || age > STALE_AFTER_TICKS) {
            return Selection.unavailable("stale", age);
        }
        return new Selection(stored.payload(), true, "matched", age,
                RECEIVED.get(), REJECTED.get());
    }

    public static void reset() {
        latest = null;
        RECEIVED.set(0L);
        REJECTED.set(0L);
    }

    static boolean isWellFormed(WindSyncPayload payload) {
        return payload != null
                && payload.dimension() != null
                && payload.regionSize() > 0
                && Float.isFinite(payload.speedMps())
                && Float.isFinite(payload.directionDeg())
                && payload.speedMps() >= 0.0F;
    }

    private static boolean matches(WindSyncPayload payload, RegionInstanceKey region) {
        return region != null
                && payload.regionX() == region.regionX()
                && payload.regionZ() == region.regionZ()
                && payload.regionSize() == region.regionSize();
    }

    private static long age(long currentTick, long receivedTick) {
        return currentTick - receivedTick;
    }

    private record StoredSample(WindSyncPayload payload, long localTick) {
    }

    public record Selection(WindSyncPayload payload, boolean available, String match,
                            long ageTicks, long receivedCount, long rejectedCount) {
        private static Selection unavailable(String match, long age) {
            return new Selection(null, false, match, age, RECEIVED.get(), REJECTED.get());
        }
    }
}
