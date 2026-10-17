package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SyncSwapPoolS2C(UUID subject, Map<ResourceLocation, List<ResourceLocation>> available)
    implements CustomPacketPayload {

    public static final Type<SyncSwapPoolS2C> TYPE = new Type<>(Origins.id("sync_swap_pool"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSwapPoolS2C> STREAM_CODEC = StreamCodec.of(
        SyncSwapPoolS2C::write, SyncSwapPoolS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncSwapPoolS2C payload) {
        buf.writeUUID(payload.subject);
        buf.writeVarInt(payload.available.size());
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> e : payload.available.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue().size());
            for (ResourceLocation id : e.getValue()) buf.writeResourceLocation(id);
        }
    }

    private static SyncSwapPoolS2C read(RegistryFriendlyByteBuf buf) {
        UUID subject = buf.readUUID();
        int n = buf.readVarInt();
        Map<ResourceLocation, List<ResourceLocation>> available = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            ResourceLocation layer = buf.readResourceLocation();
            int size = buf.readVarInt();
            List<ResourceLocation> ids = new ArrayList<>(size);
            for (int j = 0; j < size; j++) ids.add(buf.readResourceLocation());
            available.put(layer, ids);
        }
        return new SyncSwapPoolS2C(subject, available);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
