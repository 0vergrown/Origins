package dev.overgrown.origins.network;

import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.client.screen.ChooseOriginScreen;
import dev.overgrown.origins.client.screen.WaitForNextLayerScreen;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class OriginsClientNetwork {
    private OriginsClientNetwork() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OriginsPackets.SYNC_REGISTRIES, (client, handler, buf, sender) -> {
            List<Origin> origins = new ArrayList<>();
            int oc = buf.readVarInt();
            for (int i = 0; i < oc; i++) origins.add(Origin.read(buf));
            List<OriginLayer> layers = new ArrayList<>();
            int lc = buf.readVarInt();
            for (int i = 0; i < lc; i++) layers.add(OriginLayer.read(buf));
            client.execute(() -> {
                OriginRegistry.replaceAll(origins);
                OriginLayers.replaceAll(layers);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OriginsPackets.SYNC_BADGES, (client, handler, buf, sender) -> {
            int powerCount = buf.readVarInt();
            Map<ResourceLocation, List<dev.overgrown.origins.badge.Badge>> badges = new HashMap<>();
            for (int i = 0; i < powerCount; i++) {
                ResourceLocation id = buf.readResourceLocation();
                int badgeCount = buf.readVarInt();
                List<dev.overgrown.origins.badge.Badge> list = new ArrayList<>(badgeCount);
                for (int j = 0; j < badgeCount; j++) list.add(dev.overgrown.origins.badge.Badge.fromNetwork(buf));
                badges.put(id, list);
            }
            client.execute(() -> dev.overgrown.origins.client.BadgeClientState.replaceAll(badges));
        });

        ClientPlayNetworking.registerGlobalReceiver(OriginsPackets.SYNC_PLAYER_ORIGINS, (client, handler, buf, sender) -> {
            UUID subject = buf.readUUID();
            int n = buf.readVarInt();
            Map<ResourceLocation, ResourceLocation> picks = new HashMap<>();
            for (int i = 0; i < n; i++) {
                picks.put(buf.readResourceLocation(), buf.readResourceLocation());
            }
            client.execute(() -> OriginsClientState.setOrigins(subject, picks));
        });

        ClientPlayNetworking.registerGlobalReceiver(OriginsPackets.OPEN_CHOOSE_SCREEN, (client, handler, buf, sender) -> {
            ResourceLocation layerId = buf.readResourceLocation();
            boolean fromOrb = buf.readBoolean();
            client.execute(() -> {
                OriginLayer layer = OriginLayers.get(layerId);
                if (layer == null) return;
                Minecraft.getInstance().setScreen(new ChooseOriginScreen(layer, fromOrb));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OriginsPackets.CLOSE_CHOOSE_SCREEN, (client, handler, buf, sender) -> {
            client.execute(() -> {
                if (Minecraft.getInstance().screen instanceof ChooseOriginScreen
                    || Minecraft.getInstance().screen instanceof WaitForNextLayerScreen) {
                    Minecraft.getInstance().setScreen(null);
                }
            });
        });
    }

    public static void sendChoose(ResourceLocation layerId, ResourceLocation originId, boolean fromOrb) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(layerId);
        buf.writeResourceLocation(originId);
        buf.writeBoolean(fromOrb);
        ClientPlayNetworking.send(OriginsPackets.CHOOSE_ORIGIN, buf);
    }
}
