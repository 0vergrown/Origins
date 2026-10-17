package dev.overgrown.origins.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class OriginsClientState {
    private static final Map<UUID, Map<ResourceLocation, ResourceLocation>> ORIGINS_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, ResourceLocation>> SWAPS_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, List<ResourceLocation>>> POOL_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Map<ResourceLocation, List<ResourceLocation>>> AVAILABLE_BY_PLAYER = new HashMap<>();

    private OriginsClientState() {}

    public static void setSwaps(UUID player, Map<ResourceLocation, ResourceLocation> swaps) {
        SWAPS_BY_PLAYER.put(player, Map.copyOf(swaps));
    }

    public static Map<ResourceLocation, ResourceLocation> getSwaps(UUID player) {
        return SWAPS_BY_PLAYER.getOrDefault(player, Map.of());
    }

    public static void setPool(UUID player, Map<ResourceLocation, List<ResourceLocation>> pool) {
        POOL_BY_PLAYER.put(player, Map.copyOf(pool));
    }

    public static Map<ResourceLocation, List<ResourceLocation>> getPool(UUID player) {
        return POOL_BY_PLAYER.getOrDefault(player, Map.of());
    }

    public static void setAvailable(UUID player, Map<ResourceLocation, List<ResourceLocation>> available) {
        AVAILABLE_BY_PLAYER.put(player, Map.copyOf(available));
    }

    public static Map<ResourceLocation, List<ResourceLocation>> getAvailable(UUID player) {
        return AVAILABLE_BY_PLAYER.getOrDefault(player, Map.of());
    }

    public static void setOrigins(UUID player, Map<ResourceLocation, ResourceLocation> picks) {
        ORIGINS_BY_PLAYER.put(player, Map.copyOf(picks));
    }

    public static Map<ResourceLocation, ResourceLocation> get(UUID player) {
        return ORIGINS_BY_PLAYER.getOrDefault(player, Map.of());
    }

    public static void forget(UUID player) {
        ORIGINS_BY_PLAYER.remove(player);
        SWAPS_BY_PLAYER.remove(player);
        POOL_BY_PLAYER.remove(player);
        AVAILABLE_BY_PLAYER.remove(player);
    }

    public static void clear() {
        ORIGINS_BY_PLAYER.clear();
        SWAPS_BY_PLAYER.clear();
        POOL_BY_PLAYER.clear();
        AVAILABLE_BY_PLAYER.clear();
    }
}
