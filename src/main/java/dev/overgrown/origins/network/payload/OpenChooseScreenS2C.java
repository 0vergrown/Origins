package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public record OpenChooseScreenS2C(ResourceLocation layerId, boolean fromOrb) implements CustomPacketPayload {
    public static final Type<OpenChooseScreenS2C> TYPE = new Type<>(Origins.id("open_choose_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChooseScreenS2C> STREAM_CODEC = StreamCodec.of(
        OpenChooseScreenS2C::write, OpenChooseScreenS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, OpenChooseScreenS2C payload) {
        buf.writeResourceLocation(payload.layerId);
        buf.writeBoolean(payload.fromOrb);
    }

    private static OpenChooseScreenS2C read(RegistryFriendlyByteBuf buf) {
        return new OpenChooseScreenS2C(buf.readResourceLocation(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
