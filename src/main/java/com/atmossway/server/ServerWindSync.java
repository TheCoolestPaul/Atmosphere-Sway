package com.atmossway.server;

import com.atmossway.network.WindSyncPayload;
import net.Gabou.projectatmosphere.api.WindVectorApi;
import net.Gabou.projectatmosphere.util.RegionInstanceKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.HashMap;
import java.util.Map;

public final class ServerWindSync {
    public static final int SEND_INTERVAL_TICKS = 5;

    private ServerWindSync() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!shouldSend(event.getServer().getTickCount())) {
            return;
        }

        Map<SampleKey, WindSyncPayload> samples = new HashMap<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!NetworkRegistry.hasChannel(player.connection, WindSyncPayload.TYPE.id())) {
                continue;
            }
            ServerLevel level = player.serverLevel();
            RegionInstanceKey region = RegionInstanceKey.from(player.blockPosition());
            SampleKey key = new SampleKey(level.dimension(), region);
            WindSyncPayload payload = samples.computeIfAbsent(key, ignored -> sample(level, region));
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    static boolean shouldSend(long serverTick) {
        return Math.floorMod(serverTick, SEND_INTERVAL_TICKS) == 0;
    }

    static boolean isValid(float speedMps, float directionDeg) {
        return Float.isFinite(speedMps)
                && Float.isFinite(directionDeg)
                && speedMps >= 0.0F;
    }

    private static WindSyncPayload sample(ServerLevel level, RegionInstanceKey region) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return payload(level, region, 0.0F, 0.0F, false);
        }
        WindVectorApi.WindSample sample = WindVectorApi.getSurface(region, level.getGameTime());
        boolean valid = isValid(sample.speedMps(), sample.directionDeg());
        return payload(
                level,
                region,
                valid ? sample.speedMps() : 0.0F,
                valid ? sample.directionDeg() : 0.0F,
                valid
        );
    }

    private static WindSyncPayload payload(ServerLevel level, RegionInstanceKey region,
                                           float speedMps, float directionDeg, boolean valid) {
        return new WindSyncPayload(
                level.dimension().location(),
                region.regionX(),
                region.regionZ(),
                region.regionSize(),
                level.getGameTime(),
                speedMps,
                directionDeg,
                valid
        );
    }

    record SampleKey(ResourceKey<Level> dimension, RegionInstanceKey region) {
    }
}
