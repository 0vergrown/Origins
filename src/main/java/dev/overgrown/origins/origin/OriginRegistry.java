package dev.overgrown.origins.origin;

import dev.overgrown.origins.OriginsWorldConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class OriginRegistry {
    private static final Map<ResourceLocation, Origin> BY_ID = new HashMap<>();
    private static boolean anyUpgrades;
    public static final ResourceLocation EMPTY_ID = ResourceLocation.fromNamespaceAndPath("origins", "empty");

    static {

        BY_ID.put(EMPTY_ID, Origin.empty(EMPTY_ID));
    }

    private OriginRegistry() {}

    public static void register(Origin origin) {
        BY_ID.put(origin.id(), origin);
        if (!origin.upgrades().isEmpty()) anyUpgrades = true;
    }

    public static boolean anyUpgrades() {
        return anyUpgrades;
    }

    public static @Nullable Origin get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static Origin getOrEmpty(ResourceLocation id) {
        Origin o = BY_ID.get(id);
        return o != null ? o : Origin.empty(EMPTY_ID);
    }

    public static boolean contains(ResourceLocation id) {
        return BY_ID.containsKey(id);
    }

    public static Collection<Origin> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static int size() {
        return BY_ID.size();
    }

    public static void replaceAll(Collection<Origin> origins, @Nullable MinecraftServer server) {
        var cfg = OriginsWorldConfig.get(server);

        anyUpgrades = false;
        for (Origin origin : origins) {
            if (!origin.upgrades().isEmpty()) {
                anyUpgrades = true;
                break;
            }
        }
        store(cfg.filterOrigins(origins), cfg);
    }

    public static void acceptSynced(Collection<Origin> origins) {
        store(origins, OriginsWorldConfig.get(null));
    }

    private static void store(Collection<Origin> origins, OriginsWorldConfig cfg) {
        BY_ID.clear();
        BY_ID.put(EMPTY_ID, Origin.empty(EMPTY_ID));
        for (Origin origin : origins) {
            var original = origin.powerEntries();

            BY_ID.put(origin.id(), origin.setPowerEntries(cfg.filterPowers(original, origin.id())));
        }
    }
}
