package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;


public record ChooseOriginC2S(ResourceLocation layerId, ResourceLocation originId, boolean fromOrb)
    implements CustomPacketPayload {

    public static final Type<ChooseOriginC2S> TYPE = new Type<>(Origins.id("choose_origin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseOriginC2S> STREAM_CODEC = StreamCodec.of(
        ChooseOriginC2S::write, ChooseOriginC2S::read);

    private static void write(RegistryFriendlyByteBuf buf, ChooseOriginC2S payload) {
        buf.writeResourceLocation(payload.layerId);
        buf.writeResourceLocation(payload.originId);
        buf.writeBoolean(payload.fromOrb);
    }

    private static ChooseOriginC2S read(RegistryFriendlyByteBuf buf) {
        return new ChooseOriginC2S(buf.readResourceLocation(), buf.readResourceLocation(), buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
