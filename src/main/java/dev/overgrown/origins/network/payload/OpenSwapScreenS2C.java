package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenSwapScreenS2C(ResourceLocation layerId) implements CustomPacketPayload {

    public static final Type<OpenSwapScreenS2C> TYPE = new Type<>(Origins.id("open_swap_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSwapScreenS2C> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeResourceLocation(payload.layerId),
        buf -> new OpenSwapScreenS2C(buf.readResourceLocation()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
