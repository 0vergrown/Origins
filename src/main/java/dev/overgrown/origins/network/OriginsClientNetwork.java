package dev.overgrown.origins.network;

import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.client.screen.ChooseOriginScreen;
import dev.overgrown.origins.client.screen.WaitForNextLayerScreen;
import dev.overgrown.origins.network.payload.ChooseOriginC2S;
import dev.overgrown.origins.network.payload.CloseChooseScreenS2C;
import dev.overgrown.origins.client.BadgeClientState;
import dev.overgrown.origins.network.payload.OpenChooseScreenS2C;
import dev.overgrown.origins.network.payload.SyncBadgesS2C;
import dev.overgrown.origins.network.payload.SyncPlayerOriginsS2C;
import dev.overgrown.origins.network.payload.SyncRegistriesS2C;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class OriginsClientNetwork {
    private OriginsClientNetwork() {}

    public static void handleSyncRegistries(SyncRegistriesS2C payload) {
        OriginRegistry.replaceAll(payload.origins());
        OriginLayers.replaceAll(payload.layers());
    }

    public static void handleSyncBadges(SyncBadgesS2C payload) {
        BadgeClientState.replaceAll(payload.badgesByPower());
    }

    public static void handleSyncPlayerOrigins(SyncPlayerOriginsS2C payload) {
        OriginsClientState.setOrigins(payload.subject(), payload.picks());
    }

    public static void handleSyncPlayerSwaps(dev.overgrown.origins.network.payload.SyncPlayerSwapsS2C payload) {
        OriginsClientState.setSwaps(payload.subject(), payload.swaps());
        OriginsClientState.setPool(payload.subject(), payload.pool());
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null || !client.player.getUUID().equals(payload.subject())) return;
        if (client.screen instanceof dev.overgrown.origins.client.screen.ViewOriginScreen view) {
            view.refresh();
        } else if (client.screen instanceof dev.overgrown.origins.client.screen.SwapOriginScreen swap) {
            swap.refresh();
        }
    }

    public static void handleOpenSwapScreen(dev.overgrown.origins.network.payload.OpenSwapScreenS2C payload) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new dev.overgrown.origins.client.screen.SwapOriginScreen(payload.layerId()));
    }

    public static void handleOriginRoll(dev.overgrown.origins.network.payload.OriginRollS2C payload) {
        dev.overgrown.origins.client.OriginRollQueue.enqueue(payload.layerId(), payload.originId(), payload.duration());
    }

    public static void handleOpenChooseScreen(OpenChooseScreenS2C payload) {
        OriginLayer layer = OriginLayers.get(payload.layerId());
        if (layer == null) return;
        Minecraft.getInstance().setScreen(new ChooseOriginScreen(layer, payload.fromOrb()));
    }

    public static void handleCloseChooseScreen(CloseChooseScreenS2C payload) {
        if (Minecraft.getInstance().screen instanceof ChooseOriginScreen
            || Minecraft.getInstance().screen instanceof WaitForNextLayerScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static void sendChoose(ResourceLocation layerId, ResourceLocation originId, boolean fromOrb) {
        PacketDistributor.sendToServer(new ChooseOriginC2S(layerId, originId, fromOrb));
    }
}
