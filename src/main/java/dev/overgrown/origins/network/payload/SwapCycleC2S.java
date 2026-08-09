package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SwapCycleC2S(boolean toMain) implements CustomPacketPayload {

    public static final Type<SwapCycleC2S> TYPE = new Type<>(Origins.id("swap_cycle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwapCycleC2S> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeBoolean(payload.toMain), buf -> new SwapCycleC2S(buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
