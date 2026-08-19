package com.atmossway.network;

import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class WindSyncNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private WindSyncNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .optional()
                .playToClient(
                        WindSyncPayload.TYPE,
                        WindSyncPayload.STREAM_CODEC,
                        (payload, context) -> ClientWindSyncState.accept(
                                payload,
                                context.player().level().dimension().location(),
                                RegionInstanceKey.from(context.player().blockPosition()),
                                context.player().level().getGameTime()
                        )
                );
    }
}
