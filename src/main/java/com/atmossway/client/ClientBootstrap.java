package com.atmossway.client;

import com.atmossway.AtmosSway;
import com.github.razorplay01.sway.api.SwayAPI;
import com.github.razorplay01.sway.platform.neoforge.util.SwayModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@EventBusSubscriber(modid = AtmosSway.MOD_ID, value = Dist.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FoliageRegistrar.register();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeModelBaking(ModelEvent.ModifyBakingResult event) {
        FoliageRegistrar.register();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterModelBaking(ModelEvent.ModifyBakingResult event) {
        long wrappedModels = event.getModels().values().stream()
                .filter(SwayModel.class::isInstance)
                .count();
        AtmosSwayDiagnostics.modelWrappingComplete(
                SwayAPI.getRegistry().size(), wrappedModels
        );
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
