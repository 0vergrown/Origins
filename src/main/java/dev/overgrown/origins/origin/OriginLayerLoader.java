package dev.overgrown.origins.origin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginLayerLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonObject>> {
    private static final String DIR = "origin_layers";

    public OriginLayerLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, List<JsonDataFiles.Source>> stacks = JsonDataFiles.stacks(resourceManager, DIR, Origins.LOGGER);
        Map<ResourceLocation, JsonObject> merged = new HashMap<>(stacks.size());
        for (Map.Entry<ResourceLocation, List<JsonDataFiles.Source>> entry : stacks.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonObject layer = null;
            for (JsonDataFiles.Source source : entry.getValue()) {
                JsonElement json = JsonDataFiles.read(source, id, Origins.LOGGER);
                if (json == null || !json.isJsonObject()) continue;
                JsonObject version = json.getAsJsonObject();
                if (layer == null || GsonHelper.getAsBoolean(version, "replace", false)) {
                    layer = version;
                } else {
                    mergeInto(layer, version);
                }
            }
            if (layer != null) merged.put(id, layer);
        }
        return merged;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonObject> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<OriginLayer> layers = new ArrayList<>(entries.size());
        for (Map.Entry<ResourceLocation, JsonObject> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonObject merged = entry.getValue();
            try {
                if (!ApoliReloadListener.loadConditionPasses(
                    new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE, merged), id)) continue;
                layers.add(OriginLayer.fromJson(id, (JsonObject) IdWildcards.apply(merged, id)));
            } catch (Exception e) {
                Origins.LOGGER.error("Failed to load origin layer {}: {}", id, e.getMessage());
            }
        }
        OriginLayers.replaceAll(layers);
        Origins.LOGGER.info("Loaded {} origin layers.", OriginLayers.size());
    }

    private static void mergeInto(JsonObject acc, JsonObject src) {
        JsonArray origins = acc.has("origins") && acc.get("origins").isJsonArray()
            ? acc.getAsJsonArray("origins") : new JsonArray();
        Set<String> seen = new HashSet<>();
        for (JsonElement e : origins) seen.add(e.toString());
        if (src.has("origins") && src.get("origins").isJsonArray()) {
            for (JsonElement e : src.getAsJsonArray("origins")) {
                if (seen.add(e.toString())) origins.add(e);
            }
        }
        acc.add("origins", origins);

        for (Map.Entry<String, JsonElement> e : src.entrySet()) {
            String key = e.getKey();
            if (key.equals("origins") || key.equals("replace")) continue;
            acc.add(key, e.getValue());
        }
    }
}
