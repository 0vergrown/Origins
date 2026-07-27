package dev.overgrown.origins.origin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OriginLayerLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIR = "origin_layers";

    public OriginLayerLoader() {
        super(GSON, DIR);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<OriginLayer> layers = new ArrayList<>();
        for (ResourceLocation id : entries.keySet()) {
            ResourceLocation fullPath = new ResourceLocation(
                id.getNamespace(), DIR + "/" + id.getPath() + ".json");
            try {
                JsonObject merged = mergeStack(resourceManager.getResourceStack(fullPath), id);
                if (merged == null && entries.get(id).isJsonObject()) {
                    merged = entries.get(id).getAsJsonObject();
                }
                if (merged != null) layers.add(OriginLayer.fromJson(id, merged));
            } catch (Exception e) {
                Origins.LOGGER.error("Failed to load origin layer {}: {}", id, e.getMessage());
            }
        }
        OriginLayers.replaceAll(layers);
        Origins.LOGGER.info("Loaded {} origin layers.", OriginLayers.size());
    }

    private static JsonObject mergeStack(List<Resource> stack, ResourceLocation id) {
        JsonObject acc = null;
        for (Resource resource : stack) {
            JsonObject json;
            try (Reader reader = resource.openAsReader()) {
                JsonElement el = GsonHelper.parse(reader, true);
                if (!el.isJsonObject()) continue;
                json = el.getAsJsonObject();
            } catch (Exception e) {
                Origins.LOGGER.error("Failed to read a source of origin layer {}: {}", id, e.getMessage());
                continue;
            }
            if (acc == null || GsonHelper.getAsBoolean(json, "replace", false)) {
                acc = json.deepCopy();
            } else {
                mergeInto(acc, json);
            }
        }
        return acc;
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
