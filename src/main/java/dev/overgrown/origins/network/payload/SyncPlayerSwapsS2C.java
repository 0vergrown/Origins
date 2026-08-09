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

public record SyncPlayerSwapsS2C(UUID subject,
                                 Map<ResourceLocation, ResourceLocation> swaps,
                                 Map<ResourceLocation, List<ResourceLocation>> pool)
    implements CustomPacketPayload {

    public static final Type<SyncPlayerSwapsS2C> TYPE = new Type<>(Origins.id("sync_player_swaps"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerSwapsS2C> STREAM_CODEC = StreamCodec.of(
        SyncPlayerSwapsS2C::write, SyncPlayerSwapsS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncPlayerSwapsS2C payload) {
        buf.writeUUID(payload.subject);
        buf.writeVarInt(payload.swaps.size());
        for (Map.Entry<ResourceLocation, ResourceLocation> e : payload.swaps.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeResourceLocation(e.getValue());
        }
        buf.writeVarInt(payload.pool.size());
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> e : payload.pool.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeVarInt(e.getValue().size());
            for (ResourceLocation id : e.getValue()) buf.writeResourceLocation(id);
        }
    }

    private static SyncPlayerSwapsS2C read(RegistryFriendlyByteBuf buf) {
        UUID subject = buf.readUUID();
        int n = buf.readVarInt();
        Map<ResourceLocation, ResourceLocation> swaps = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            swaps.put(buf.readResourceLocation(), buf.readResourceLocation());
        }
        int m = buf.readVarInt();
        Map<ResourceLocation, List<ResourceLocation>> pool = new HashMap<>(m);
        for (int i = 0; i < m; i++) {
            ResourceLocation layer = buf.readResourceLocation();
            int size = buf.readVarInt();
            List<ResourceLocation> ids = new ArrayList<>(size);
            for (int j = 0; j < size; j++) ids.add(buf.readResourceLocation());
            pool.put(layer, ids);
        }
        return new SyncPlayerSwapsS2C(subject, swaps, pool);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
