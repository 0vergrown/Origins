package dev.overgrown.origins.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class OriginsClientState {
    private static final Map<UUID, Map<ResourceLocation, ResourceLocation>> ORIGINS_BY_PLAYER = new HashMap<>();

    private OriginsClientState() {}

    public static void setOrigins(UUID player, Map<ResourceLocation, ResourceLocation> picks) {
        ORIGINS_BY_PLAYER.put(player, Map.copyOf(picks));
    }

    public static Map<ResourceLocation, ResourceLocation> get(UUID player) {
        return ORIGINS_BY_PLAYER.getOrDefault(player, Map.of());
    }

    public static void forget(UUID player) {
        ORIGINS_BY_PLAYER.remove(player);
    }

    public static void clear() {
        ORIGINS_BY_PLAYER.clear();
    }
}
