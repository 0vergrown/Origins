package dev.overgrown.origins.network;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.network.payload.ChooseOriginC2S;
import dev.overgrown.origins.network.payload.CloseChooseScreenS2C;
import dev.overgrown.origins.network.payload.OpenChooseScreenS2C;
import dev.overgrown.origins.network.payload.SyncBadgesS2C;
import dev.overgrown.origins.network.payload.SyncPlayerOriginsS2C;
import dev.overgrown.origins.network.payload.SyncRegistriesS2C;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class OriginsNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private OriginsNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Origins.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(SyncRegistriesS2C.TYPE, SyncRegistriesS2C.STREAM_CODEC, OriginsNetwork::onSyncRegistries);
        registrar.playToClient(SyncBadgesS2C.TYPE, SyncBadgesS2C.STREAM_CODEC, OriginsNetwork::onSyncBadges);
        registrar.playToClient(SyncPlayerOriginsS2C.TYPE, SyncPlayerOriginsS2C.STREAM_CODEC, OriginsNetwork::onSyncPlayerOrigins);
        registrar.playToClient(OpenChooseScreenS2C.TYPE, OpenChooseScreenS2C.STREAM_CODEC, OriginsNetwork::onOpenChooseScreen);
        registrar.playToClient(CloseChooseScreenS2C.TYPE, CloseChooseScreenS2C.STREAM_CODEC, OriginsNetwork::onCloseChooseScreen);
        registrar.playToClient(dev.overgrown.origins.network.payload.OriginRollS2C.TYPE, dev.overgrown.origins.network.payload.OriginRollS2C.STREAM_CODEC, OriginsNetwork::onOriginRoll);
        registrar.playToClient(dev.overgrown.origins.network.payload.SyncPlayerSwapsS2C.TYPE,
            dev.overgrown.origins.network.payload.SyncPlayerSwapsS2C.STREAM_CODEC, OriginsNetwork::onSyncPlayerSwaps);
        registrar.playToClient(dev.overgrown.origins.network.payload.OpenSwapScreenS2C.TYPE,
            dev.overgrown.origins.network.payload.OpenSwapScreenS2C.STREAM_CODEC, OriginsNetwork::onOpenSwapScreen);
        registrar.playToServer(ChooseOriginC2S.TYPE, ChooseOriginC2S.STREAM_CODEC, OriginsNetwork::onChooseOrigin);
        registrar.playToServer(dev.overgrown.origins.network.payload.SwapCycleC2S.TYPE,
            dev.overgrown.origins.network.payload.SwapCycleC2S.STREAM_CODEC, OriginsNetwork::onSwapCycle);
        registrar.playToServer(dev.overgrown.origins.network.payload.SwapSelectC2S.TYPE,
            dev.overgrown.origins.network.payload.SwapSelectC2S.STREAM_CODEC, OriginsNetwork::onSwapSelect);
    }

    private static void onSyncPlayerSwaps(dev.overgrown.origins.network.payload.SyncPlayerSwapsS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleSyncPlayerSwaps(payload));
    }

    private static void onOpenSwapScreen(dev.overgrown.origins.network.payload.OpenSwapScreenS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleOpenSwapScreen(payload));
    }

    private static void onSwapCycle(dev.overgrown.origins.network.payload.SwapCycleC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) OriginsServerNetwork.handleSwapCycle(sp, payload.toMain());
        });
    }

    private static void onSwapSelect(dev.overgrown.origins.network.payload.SwapSelectC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                OriginsServerNetwork.handleSwapSelect(sp, payload.layerId(), payload.originId());
            }
        });
    }

    private static void onSyncRegistries(SyncRegistriesS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleSyncRegistries(payload));
    }

    private static void onSyncBadges(SyncBadgesS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleSyncBadges(payload));
    }

    private static void onSyncPlayerOrigins(SyncPlayerOriginsS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleSyncPlayerOrigins(payload));
    }

    private static void onOpenChooseScreen(OpenChooseScreenS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleOpenChooseScreen(payload));
    }

    private static void onCloseChooseScreen(CloseChooseScreenS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleCloseChooseScreen(payload));
    }

    private static void onOriginRoll(dev.overgrown.origins.network.payload.OriginRollS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OriginsClientNetwork.handleOriginRoll(payload));
    }

    private static void onChooseOrigin(ChooseOriginC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                OriginsServerNetwork.handleChoose(sp, payload.layerId(), payload.originId(), payload.fromOrb());
            }
        });
    }
}
