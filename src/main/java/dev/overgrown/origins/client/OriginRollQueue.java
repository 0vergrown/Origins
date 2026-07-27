package dev.overgrown.origins.client;

import dev.overgrown.origins.client.screen.OriginRollScreen;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class OriginRollQueue {

    private static ResourceLocation pendingLayer;
    private static ResourceLocation pendingOrigin;
    private static int pendingDuration;
    private static int pendingAge;

    private OriginRollQueue() {}

    public static void enqueue(ResourceLocation layer, ResourceLocation origin, int duration) {
        dev.overgrown.origins.Origins.LOGGER.debug("[Origins] Origin roll received: {} in layer {} — waiting for a free screen", origin, layer);
        pendingLayer = layer;
        pendingOrigin = origin;
        pendingDuration = duration;
        pendingAge = 0;
    }

    public static void clear() {
        pendingLayer = null;
    }

    public static void tick(Minecraft mc) {
        if (pendingLayer == null) return;
        if (mc.player == null || mc.level == null) {
            pendingAge = 0;
            return;
        }
        if (++pendingAge > 600) {
            dev.overgrown.origins.Origins.LOGGER.warn("[Origins] Origin roll gave up after 30s (layer {} / origin {} unresolved or a screen never freed)", pendingLayer, pendingOrigin);
            clear();
            return;
        }
        OriginLayer layer = OriginLayers.get(pendingLayer);
        Origin origin = OriginRegistry.get(pendingOrigin);
        if (layer == null || origin == null) return;
        if (mc.screen != null) return;
        ResourceLocation result = pendingOrigin;
        int duration = pendingDuration;
        clear();
        dev.overgrown.origins.Origins.LOGGER.debug("[Origins] Opening origin roll screen for {}", result);
        mc.setScreen(new OriginRollScreen(layer, result, duration));
    }
}
