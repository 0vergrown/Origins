package dev.overgrown.origins.network;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.payload.CloseChooseScreenS2C;
import dev.overgrown.origins.network.payload.OpenChooseScreenS2C;
import dev.overgrown.origins.network.payload.SyncPlayerOriginsS2C;
import dev.overgrown.origins.network.payload.SyncRegistriesS2C;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class OriginsServerNetwork {
    private OriginsServerNetwork() {}

    public static final ResourceLocation RANDOM_ORIGIN = ResourceLocation.fromNamespaceAndPath("origins", "random");

    public static void sendRegistries(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncRegistriesS2C(
            new ArrayList<>(OriginRegistry.all()), new ArrayList<>(OriginLayers.all())));
    }

    public static void sendBadges(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new dev.overgrown.origins.network.payload.SyncBadgesS2C(
            dev.overgrown.origins.badge.BadgeManager.collectForSend(player.server)));
    }

    public static void sendPlayerOriginsTo(ServerPlayer recipient, ServerPlayer subject) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(subject);
        PacketDistributor.sendToPlayer(recipient, new SyncPlayerOriginsS2C(subject.getUUID(), state.snapshot()));
    }

    public static void sendPlayerSwapsTo(ServerPlayer recipient, ServerPlayer subject) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(subject);
        PacketDistributor.sendToPlayer(recipient,
            new dev.overgrown.origins.network.payload.SyncPlayerSwapsS2C(
                subject.getUUID(), state.swapSnapshot(), state.poolSnapshot()));
    }

    public static void broadcastPlayerSwaps(MinecraftServer server, ServerPlayer subject) {
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            sendPlayerSwapsTo(recipient, subject);
        }
    }

    public static void openSwapScreen(ServerPlayer player, net.minecraft.resources.ResourceLocation targetLayerId) {
        PacketDistributor.sendToPlayer(player,
            new dev.overgrown.origins.network.payload.OpenSwapScreenS2C(targetLayerId));
    }

    public static void handleSwapCycle(ServerPlayer player, boolean toMain) {
        net.minecraft.resources.ResourceLocation target =
            dev.overgrown.origins.origin.SwapManager.resolveTarget(null);
        if (target != null) dev.overgrown.origins.origin.SwapManager.cycle(player, target, toMain);
    }

    public static void handleSwapSelect(ServerPlayer player, net.minecraft.resources.ResourceLocation layerId,
                                        net.minecraft.resources.ResourceLocation originId) {
        net.minecraft.resources.ResourceLocation target =
            dev.overgrown.origins.origin.SwapManager.resolveTarget(layerId);
        if (target == null) return;
        boolean toMain = originId.equals(dev.overgrown.origins.network.payload.SwapSelectC2S.MAIN);
        dev.overgrown.origins.origin.SwapManager.applySwap(player, target, toMain ? null : originId);
    }

    public static void broadcastPlayerOrigins(MinecraftServer server, ServerPlayer subject) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(subject);
        PacketDistributor.sendToAllPlayers(new SyncPlayerOriginsS2C(subject.getUUID(), state.snapshot()));
    }

    public static void openChooseScreen(ServerPlayer player, OriginLayer layer, boolean fromOrb) {
        PacketDistributor.sendToPlayer(player, new OpenChooseScreenS2C(layer.id(), fromOrb));
    }

    public static void closeChooseScreen(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, CloseChooseScreenS2C.INSTANCE);
    }

    public static void handleChoose(ServerPlayer player, ResourceLocation layerId,
                                    ResourceLocation originId, boolean fromOrb) {
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer == null) {
            Origins.LOGGER.warn("{} tried to choose unknown layer {}", player.getName().getString(), layerId);
            return;
        }

        boolean wasRandom = originId.equals(RANDOM_ORIGIN);
        if (wasRandom) {
            if (!layer.allowRandom()) {
                Origins.LOGGER.warn("{} tried to roll random on layer {} which doesn't allow it",
                    player.getName().getString(), layerId);
                advanceOrClose(player, fromOrb);
                return;
            }
            ResourceLocation rolled = rollRandom(player, layer);
            if (rolled == null) {
                Origins.LOGGER.warn("Random roll on layer {} produced no eligible origin", layerId);
                advanceOrClose(player, fromOrb);
                return;
            }
            originId = rolled;
        }

        Origin origin = OriginRegistry.get(originId);
        if (origin == null) {
            Origins.LOGGER.warn("{} tried to choose unknown origin {}", player.getName().getString(), originId);
            advanceOrClose(player, fromOrb);
            return;
        }
        if (!origin.choosable() && !originId.equals(OriginRegistry.EMPTY_ID)) {
            Origins.LOGGER.warn("{} tried to choose unchoosable origin {}",
                player.getName().getString(), originId);
            advanceOrClose(player, fromOrb);
            return;
        }
        OriginManager.chooseOrigin(player, layerId, originId, fromOrb);
        broadcastPlayerOrigins(player.getServer(), player);
        advanceOrClose(player, fromOrb);
        if (wasRandom) {
            sendOriginRoll(player, layer, originId);
        }
    }

    private static void advanceOrClose(ServerPlayer player, boolean fromOrb) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        OriginLayer nextUnchosen = OriginManager.firstUnchosenLayer(player, state);
        if (nextUnchosen != null) {
            openChooseScreen(player, nextUnchosen, fromOrb);
        } else {
            closeChooseScreen(player);
        }
    }

    private static ResourceLocation rollRandom(ServerPlayer player, OriginLayer layer) {
        return dev.overgrown.origins.origin.OriginRandomizer.roll(player, layer);
    }

    public static void sendOriginRoll(ServerPlayer player, OriginLayer layer, ResourceLocation pick) {
        if (layer.random().style() != OriginLayer.RandomConfig.Style.ROLL) {
            Origins.LOGGER.info("[Origins] Origin roll skipped for {}: layer {} random style is {} (needs \"roll\")",
                player.getGameProfile().getName(), layer.id(), layer.random().style());
            return;
        }
        if (!player.connection.hasChannel(dev.overgrown.origins.network.payload.OriginRollS2C.TYPE)) {
            Origins.LOGGER.warn("[Origins] Origin roll skipped for {}: client cannot receive origins:origin_roll (older Origins build on the client?)",
                player.getGameProfile().getName());
            return;
        }
        Origins.LOGGER.debug("[Origins] Origin roll sent to {}: {} in layer {} ({} ticks)",
            player.getGameProfile().getName(), pick, layer.id(), layer.random().rollDuration());
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
            new dev.overgrown.origins.network.payload.OriginRollS2C(layer.id(), pick, layer.random().rollDuration()));
    }
}
