package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OriginRollS2C(ResourceLocation layerId, ResourceLocation originId, int duration) implements CustomPacketPayload {
    public static final Type<OriginRollS2C> TYPE = new Type<>(Origins.id("origin_roll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OriginRollS2C> STREAM_CODEC = StreamCodec.of(
        OriginRollS2C::write, OriginRollS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, OriginRollS2C payload) {
        buf.writeResourceLocation(payload.layerId);
        buf.writeResourceLocation(payload.originId);
        buf.writeVarInt(payload.duration);
    }

    private static OriginRollS2C read(RegistryFriendlyByteBuf buf) {
        return new OriginRollS2C(buf.readResourceLocation(), buf.readResourceLocation(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
