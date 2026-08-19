package com.atmossway.client;

import com.atmossway.AtmosSway;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@EventBusSubscriber(modid = AtmosSway.MOD_ID, value = Dist.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(FoliageRegistrar::register);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        AmbientWindController.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level) {
            AmbientWindController.onLevelLoad(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) {
            AmbientWindController.onLevelUnload(level);
        }
    }
}
