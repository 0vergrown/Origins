package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.Badge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public record SyncBadgesS2C(Map<ResourceLocation, List<Badge>> badgesByPower) implements CustomPacketPayload {

    public static final Type<SyncBadgesS2C> TYPE = new Type<>(Origins.id("sync_badges"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBadgesS2C> STREAM_CODEC =
        StreamCodec.of(SyncBadgesS2C::write, SyncBadgesS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncBadgesS2C payload) {
        buf.writeVarInt(payload.badgesByPower.size());
        for (Map.Entry<ResourceLocation, List<Badge>> entry : payload.badgesByPower.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (Badge badge : entry.getValue()) Badge.writeNetwork(buf, badge);
        }
    }

    private static SyncBadgesS2C read(RegistryFriendlyByteBuf buf) {
        int powerCount = buf.readVarInt();
        Map<ResourceLocation, List<Badge>> map = new LinkedHashMap<>(powerCount);
        for (int i = 0; i < powerCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int badgeCount = buf.readVarInt();
            List<Badge> badges = new LinkedList<>();
            for (int j = 0; j < badgeCount; j++) badges.add(Badge.fromNetwork(buf));
            map.put(id, badges);
        }
        return new SyncBadgesS2C(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
