package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public record SyncPlayerOriginsS2C(UUID subject, Map<ResourceLocation, ResourceLocation> picks)
    implements CustomPacketPayload {

    public static final Type<SyncPlayerOriginsS2C> TYPE = new Type<>(Origins.id("sync_player_origins"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerOriginsS2C> STREAM_CODEC = StreamCodec.of(
        SyncPlayerOriginsS2C::write, SyncPlayerOriginsS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncPlayerOriginsS2C payload) {
        buf.writeUUID(payload.subject);
        buf.writeVarInt(payload.picks.size());
        for (Map.Entry<ResourceLocation, ResourceLocation> e : payload.picks.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeResourceLocation(e.getValue());
        }
    }

    private static SyncPlayerOriginsS2C read(RegistryFriendlyByteBuf buf) {
        UUID subject = buf.readUUID();
        int n = buf.readVarInt();
        Map<ResourceLocation, ResourceLocation> picks = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            picks.put(buf.readResourceLocation(), buf.readResourceLocation());
        }
        return new SyncPlayerOriginsS2C(subject, picks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
