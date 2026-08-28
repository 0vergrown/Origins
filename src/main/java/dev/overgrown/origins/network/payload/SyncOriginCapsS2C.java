package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record SyncOriginCapsS2C(Map<ResourceLocation, Map<ResourceLocation, Integer>> taken)
    implements CustomPacketPayload {

    public static final Type<SyncOriginCapsS2C> TYPE = new Type<>(Origins.id("sync_origin_caps"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOriginCapsS2C> STREAM_CODEC = StreamCodec.of(
        SyncOriginCapsS2C::write, SyncOriginCapsS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncOriginCapsS2C payload) {
        buf.writeVarInt(payload.taken.size());
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Integer>> layer : payload.taken.entrySet()) {
            buf.writeResourceLocation(layer.getKey());
            buf.writeVarInt(layer.getValue().size());
            for (Map.Entry<ResourceLocation, Integer> origin : layer.getValue().entrySet()) {
                buf.writeResourceLocation(origin.getKey());
                buf.writeVarInt(origin.getValue());
            }
        }
    }

    private static SyncOriginCapsS2C read(RegistryFriendlyByteBuf buf) {
        int layers = buf.readVarInt();
        Map<ResourceLocation, Map<ResourceLocation, Integer>> taken = new HashMap<>(layers);
        for (int i = 0; i < layers; i++) {
            ResourceLocation layerId = buf.readResourceLocation();
            int origins = buf.readVarInt();
            Map<ResourceLocation, Integer> byOrigin = new HashMap<>(origins);
            for (int j = 0; j < origins; j++) {
                byOrigin.put(buf.readResourceLocation(), buf.readVarInt());
            }
            taken.put(layerId, byOrigin);
        }
        return new SyncOriginCapsS2C(taken);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
