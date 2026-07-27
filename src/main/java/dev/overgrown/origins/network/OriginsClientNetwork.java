package dev.overgrown.origins.network;

import dev.overgrown.origins.client.BadgeClientState;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.client.screen.ChooseOriginScreen;
import dev.overgrown.origins.client.screen.WaitForNextLayerScreen;
import dev.overgrown.origins.network.payload.ChooseOriginC2S;
import dev.overgrown.origins.network.payload.CloseChooseScreenS2C;
import dev.overgrown.origins.network.payload.OpenChooseScreenS2C;
import dev.overgrown.origins.network.payload.SyncBadgesS2C;
import dev.overgrown.origins.network.payload.SyncPlayerOriginsS2C;
import dev.overgrown.origins.network.payload.SyncRegistriesS2C;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class OriginsClientNetwork {
    private OriginsClientNetwork() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SyncRegistriesS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                OriginRegistry.replaceAll(payload.origins());
                OriginLayers.replaceAll(payload.layers());
            }));

        ClientPlayNetworking.registerGlobalReceiver(SyncBadgesS2C.TYPE, (payload, context) ->
            context.client().execute(() -> BadgeClientState.replaceAll(payload.badgesByPower())));

        ClientPlayNetworking.registerGlobalReceiver(SyncPlayerOriginsS2C.TYPE, (payload, context) ->
            context.client().execute(() -> OriginsClientState.setOrigins(payload.subject(), payload.picks())));

        ClientPlayNetworking.registerGlobalReceiver(OpenChooseScreenS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                OriginLayer layer = OriginLayers.get(payload.layerId());
                if (layer == null) return;
                Minecraft.getInstance().setScreen(new ChooseOriginScreen(layer, payload.fromOrb()));
            }));

        ClientPlayNetworking.registerGlobalReceiver(dev.overgrown.origins.network.payload.OriginRollS2C.TYPE, (payload, context) ->
            context.client().execute(() -> dev.overgrown.origins.client.OriginRollQueue.enqueue(
                payload.layerId(), payload.originId(), payload.duration())));

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
            client -> dev.overgrown.origins.client.OriginRollQueue.tick(client));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
            (handler, client) -> dev.overgrown.origins.client.OriginRollQueue.clear());

        ClientPlayNetworking.registerGlobalReceiver(CloseChooseScreenS2C.TYPE, (payload, context) ->
            context.client().execute(() -> {
                if (Minecraft.getInstance().screen instanceof ChooseOriginScreen
                    || Minecraft.getInstance().screen instanceof WaitForNextLayerScreen) {
                    Minecraft.getInstance().setScreen(null);
                }
            }));
    }

    public static void sendChoose(ResourceLocation layerId, ResourceLocation originId, boolean fromOrb) {
        ClientPlayNetworking.send(new ChooseOriginC2S(layerId, originId, fromOrb));
    }
}
