package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record CloseChooseScreenS2C() implements CustomPacketPayload {
    public static final CloseChooseScreenS2C INSTANCE = new CloseChooseScreenS2C();
    public static final Type<CloseChooseScreenS2C> TYPE = new Type<>(Origins.id("close_choose_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseChooseScreenS2C> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
