package dev.overgrown.origins.origin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OriginLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public OriginLoader() {
        super(GSON, "origins");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, List<Origin>> grouped = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject json = (JsonObject) IdWildcards.apply(entry.getValue(), id);
                Origin.codec(id).codec().parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> Origins.LOGGER.error("Failed to load origin {}: {}", id, err))
                    .ifPresent(origin -> grouped.computeIfAbsent(id, k -> new ArrayList<>()).add(origin));
            } catch (Exception e) {
                Origins.LOGGER.error("Failed to load origin {}: {}", id, e.getMessage());
            }
        }
        List<Origin> winners = new ArrayList<>(grouped.size());
        for (List<Origin> versions : grouped.values()) {
            versions.sort(Comparator.comparingInt(Origin::loadingPriority).reversed());
            winners.add(versions.get(0));
        }
        OriginRegistry.replaceAll(winners);
        Origins.LOGGER.info("Loaded {} origins.", OriginRegistry.size());
    }
}
