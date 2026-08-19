package com.atmossway.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindSyncPayloadTest {
    @Test
    void codecRoundTripsNonzeroWind() {
        WindSyncPayload original = payload(12.5F, 275.0F, true);
        assertEquals(original, roundTrip(original));
    }

    @Test
    void codecRoundTripsAuthoritativeCalmWind() {
        WindSyncPayload original = payload(0.0F, 0.0F, true);
        assertEquals(original, roundTrip(original));
    }

    @Test
    void codecRoundTripsInvalidSample() {
        WindSyncPayload original = payload(0.0F, 0.0F, false);
        assertEquals(original, roundTrip(original));
    }

    private static WindSyncPayload roundTrip(WindSyncPayload payload) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.buffer(), RegistryAccess.EMPTY
        );
        WindSyncPayload.STREAM_CODEC.encode(buffer, payload);
        return WindSyncPayload.STREAM_CODEC.decode(buffer);
    }

    private static WindSyncPayload payload(float speed, float direction, boolean valid) {
        return new WindSyncPayload(
                ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"),
                2, -3, 2000, 1234L, speed, direction, valid
        );
    }
}
