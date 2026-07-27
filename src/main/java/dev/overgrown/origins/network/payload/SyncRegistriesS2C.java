package dev.overgrown.origins.network.payload;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record SyncRegistriesS2C(List<Origin> origins, List<OriginLayer> layers) implements CustomPacketPayload {
    public static final Type<SyncRegistriesS2C> TYPE = new Type<>(Origins.id("sync_registries"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRegistriesS2C> STREAM_CODEC = StreamCodec.of(
        SyncRegistriesS2C::write, SyncRegistriesS2C::read);

    private static void write(RegistryFriendlyByteBuf buf, SyncRegistriesS2C payload) {
        buf.writeVarInt(payload.origins.size());
        for (Origin o : payload.origins) o.write(buf);
        buf.writeVarInt(payload.layers.size());
        for (OriginLayer l : payload.layers) l.write(buf);
    }

    private static SyncRegistriesS2C read(RegistryFriendlyByteBuf buf) {
        int oc = buf.readVarInt();
        List<Origin> origins = new ArrayList<>(oc);
        for (int i = 0; i < oc; i++) origins.add(Origin.read(buf));
        int lc = buf.readVarInt();
        List<OriginLayer> layers = new ArrayList<>(lc);
        for (int i = 0; i < lc; i++) layers.add(OriginLayer.read(buf));
        return new SyncRegistriesS2C(origins, layers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
