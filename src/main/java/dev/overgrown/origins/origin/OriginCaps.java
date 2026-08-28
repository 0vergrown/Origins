package dev.overgrown.origins.origin;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OriginCaps {

    public static final int INHERIT = -1;
    public static final int UNLIMITED = 0;

    private static final Map<ResourceLocation, Map<ResourceLocation, Integer>> TAKEN = new ConcurrentHashMap<>();

    private OriginCaps() {}

    public static int limitOf(ResourceLocation layerId, ResourceLocation originId) {
        Origin origin = OriginRegistry.get(originId);
        int declared = origin == null ? INHERIT : origin.maxPlayers();
        if (declared != INHERIT) return Math.max(UNLIMITED, declared);
        OriginLayer layer = OriginLayers.get(layerId);
        return layer == null ? UNLIMITED : Math.max(UNLIMITED, layer.maxPlayersPerOrigin());
    }

    public static boolean capped(ResourceLocation layerId, ResourceLocation originId) {
        return limitOf(layerId, originId) > UNLIMITED;
    }

    public static int taken(ResourceLocation layerId, ResourceLocation originId) {
        Map<ResourceLocation, Integer> byOrigin = TAKEN.get(layerId);
        if (byOrigin == null) return 0;
        Integer count = byOrigin.get(originId);
        return count == null ? 0 : count;
    }

    public static int remaining(ResourceLocation layerId, ResourceLocation originId) {
        int limit = limitOf(layerId, originId);
        if (limit <= UNLIMITED) return Integer.MAX_VALUE;
        return Math.max(0, limit - taken(layerId, originId));
    }

    public static boolean isFull(ResourceLocation layerId, ResourceLocation originId) {
        int limit = limitOf(layerId, originId);
        return limit > UNLIMITED && taken(layerId, originId) >= limit;
    }

    public static void replaceAll(Map<ResourceLocation, Map<ResourceLocation, Integer>> counts) {
        TAKEN.clear();
        counts.forEach((layerId, byOrigin) -> {
            if (!byOrigin.isEmpty()) TAKEN.put(layerId, Map.copyOf(byOrigin));
        });
    }

    public static Map<ResourceLocation, Map<ResourceLocation, Integer>> snapshot() {
        return new HashMap<>(TAKEN);
    }

    public static void clear() {
        TAKEN.clear();
    }
}
