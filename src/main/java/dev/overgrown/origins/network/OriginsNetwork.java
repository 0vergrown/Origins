package dev.overgrown.origins.network;

import dev.overgrown.origins.network.payload.ChooseOriginC2S;
import dev.overgrown.origins.network.payload.CloseChooseScreenS2C;
import dev.overgrown.origins.network.payload.OpenChooseScreenS2C;
import dev.overgrown.origins.network.payload.SyncBadgesS2C;
import dev.overgrown.origins.network.payload.SyncPlayerOriginsS2C;
import dev.overgrown.origins.network.payload.SyncRegistriesS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class OriginsNetwork {
    private OriginsNetwork() {}

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(SyncRegistriesS2C.TYPE, SyncRegistriesS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBadgesS2C.TYPE, SyncBadgesS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPlayerOriginsS2C.TYPE, SyncPlayerOriginsS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenChooseScreenS2C.TYPE, OpenChooseScreenS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(dev.overgrown.origins.network.payload.OriginRollS2C.TYPE, dev.overgrown.origins.network.payload.OriginRollS2C.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CloseChooseScreenS2C.TYPE, CloseChooseScreenS2C.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ChooseOriginC2S.TYPE, ChooseOriginC2S.STREAM_CODEC);
    }
}
