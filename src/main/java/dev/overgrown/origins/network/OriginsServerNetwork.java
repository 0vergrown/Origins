package dev.overgrown.origins.network;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class OriginsServerNetwork {
    private OriginsServerNetwork() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(OriginsPackets.CHOOSE_ORIGIN, (server, player, handler, buf, sender) -> {
            ResourceLocation layerId = buf.readResourceLocation();
            ResourceLocation originId = buf.readResourceLocation();
            boolean fromOrb = buf.readBoolean();
            server.execute(() -> handleChoose(player, layerId, originId, fromOrb));
        });
    }

    public static void sendRegistries(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        Collection<Origin> origins = OriginRegistry.all();
        buf.writeVarInt(origins.size());
        for (Origin o : origins) o.write(buf);
        Collection<OriginLayer> layers = OriginLayers.all();
        buf.writeVarInt(layers.size());
        for (OriginLayer l : layers) l.write(buf);
        ServerPlayNetworking.send(player, OriginsPackets.SYNC_REGISTRIES, buf);
    }

    public static void sendBadges(ServerPlayer player) {
        Map<ResourceLocation, List<dev.overgrown.origins.badge.Badge>> byPower =
            dev.overgrown.origins.badge.BadgeManager.collectForSend(player.server);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(byPower.size());
        for (Map.Entry<ResourceLocation, List<dev.overgrown.origins.badge.Badge>> entry : byPower.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (dev.overgrown.origins.badge.Badge badge : entry.getValue()) {
                dev.overgrown.origins.badge.Badge.writeNetwork(buf, badge);
            }
        }
        ServerPlayNetworking.send(player, OriginsPackets.SYNC_BADGES, buf);
    }

    public static void sendPlayerOriginsTo(ServerPlayer recipient, ServerPlayer subject) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(subject);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(subject.getUUID());
        var map = state.snapshot();
        buf.writeVarInt(map.size());
        for (var e : map.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeResourceLocation(e.getValue());
        }
        ServerPlayNetworking.send(recipient, OriginsPackets.SYNC_PLAYER_ORIGINS, buf);
    }

    public static void broadcastPlayerOrigins(MinecraftServer server, ServerPlayer subject) {
        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            sendPlayerOriginsTo(recipient, subject);
        }
    }

    public static void openChooseScreen(ServerPlayer player, OriginLayer layer, boolean fromOrb) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(layer.id());
        buf.writeBoolean(fromOrb);
        ServerPlayNetworking.send(player, OriginsPackets.OPEN_CHOOSE_SCREEN, buf);
    }

    public static final ResourceLocation RANDOM_ORIGIN = new ResourceLocation("origins", "random");

    private static void handleChoose(ServerPlayer player, ResourceLocation layerId,
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

    public static void closeChooseScreen(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, OriginsPackets.CLOSE_CHOOSE_SCREEN, buf);
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
        if (!ServerPlayNetworking.canSend(player, OriginsPackets.ORIGIN_ROLL)) {
            Origins.LOGGER.warn("[Origins] Origin roll skipped for {}: client cannot receive origins:origin_roll (older Origins build on the client?)",
                player.getGameProfile().getName());
            return;
        }
        Origins.LOGGER.debug("[Origins] Origin roll sent to {}: {} in layer {} ({} ticks)",
            player.getGameProfile().getName(), pick, layer.id(), layer.random().rollDuration());
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeResourceLocation(layer.id());
        buf.writeResourceLocation(pick);
        buf.writeVarInt(layer.random().rollDuration());
        ServerPlayNetworking.send(player, OriginsPackets.ORIGIN_ROLL, buf);
    }
}
