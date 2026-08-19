package com.atmossway;

import com.atmossway.config.AtmosSwayConfig;
import com.atmossway.network.WindSyncNetwork;
import com.atmossway.server.ServerWindSync;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AtmosSway.MOD_ID)
public final class AtmosSway {
    public static final String MOD_ID = "atmossway";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public AtmosSway(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, AtmosSwayConfig.SPEC);
        container.getEventBus().addListener(WindSyncNetwork::register);
        NeoForge.EVENT_BUS.addListener(ServerWindSync::onServerTick);
    }
}
