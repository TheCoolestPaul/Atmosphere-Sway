package com.atmossway.client;

import com.atmossway.AtmosSway;
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.registry.SwayRegistry;

final class FoliageRegistrar {
    private static boolean registered;

    private FoliageRegistrar() {
    }

    static synchronized void register() {
        if (registered) {
            return;
        }

        SwayRegistry.initialize();
        AtmosSway.LOGGER.info("Initialized SWAY's registry with {} directly registered blocks",
                SwayAPI.getRegistry().size());
        registered = true;
    }
}
