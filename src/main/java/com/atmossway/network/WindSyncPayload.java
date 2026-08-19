package com.atmossway.network;

import com.atmossway.AtmosSway;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WindSyncPayload(
        ResourceLocation dimension,
        int regionX,
        int regionZ,
        int regionSize,
        long serverTick,
        float speedMps,
        float directionDeg,
        boolean valid
) implements CustomPacketPayload {
    public static final Type<WindSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AtmosSway.MOD_ID, "wind_sync")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, WindSyncPayload> STREAM_CODEC =
            StreamCodec.of(WindSyncPayload::encode, WindSyncPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buffer, WindSyncPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.dimension());
        buffer.writeInt(payload.regionX());
        buffer.writeInt(payload.regionZ());
        buffer.writeInt(payload.regionSize());
        buffer.writeLong(payload.serverTick());
        buffer.writeFloat(payload.speedMps());
        buffer.writeFloat(payload.directionDeg());
        buffer.writeBoolean(payload.valid());
    }

    private static WindSyncPayload decode(RegistryFriendlyByteBuf buffer) {
        return new WindSyncPayload(
                ResourceLocation.STREAM_CODEC.decode(buffer),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readLong(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
