package dev.overgrown.origins.origin;

import dev.overgrown.origins.OriginsWorldConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OriginLayers {
    private static final Map<ResourceLocation, OriginLayer> BY_ID = new HashMap<>();

    private OriginLayers() {}

    public static @Nullable OriginLayer get(ResourceLocation id) {
        return BY_ID.get(id);
    }

    public static boolean contains(ResourceLocation id) {
        return BY_ID.containsKey(id);
    }

    public static Collection<OriginLayer> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static int size() {
        return BY_ID.size();
    }

    public static List<OriginLayer> enabledOrdered() {
        List<OriginLayer> out = new ArrayList<>();
        for (OriginLayer l : BY_ID.values()) {
            if (l.enabled()) out.add(l);
        }
        Collections.sort(out);
        return out;
    }

    public static List<OriginLayer> enabledFor(Player player) {
        List<OriginLayer> out = new ArrayList<>();
        for (OriginLayer l : enabledOrdered()) {
            if (!l.availableOrigins(player).isEmpty()) out.add(l);
        }
        return out;
    }

    public static void replaceAll(Collection<OriginLayer> layers, @Nullable MinecraftServer server) {
        var cfg = OriginsWorldConfig.get(server);
        layers = cfg.filterLayers(layers);

        BY_ID.clear();
        for (OriginLayer layer : layers) {
            var original = layer.conditionedOrigins();

            BY_ID.put(layer.id(), layer.setConditionedOrigins(cfg.filterLayerOrigins(original, layer.id())));
        }
    }
}
