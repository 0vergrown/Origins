package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SwapSelectC2S(ResourceLocation layerId, ResourceLocation originId) implements CustomPacketPayload {

    public static final ResourceLocation MAIN = Origins.id("main");

    public static final Type<SwapSelectC2S> TYPE = new Type<>(Origins.id("swap_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapSelectC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeResourceLocation(payload.layerId);
            buf.writeResourceLocation(payload.originId);
        },
        buf -> new SwapSelectC2S(buf.readResourceLocation(), buf.readResourceLocation()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
