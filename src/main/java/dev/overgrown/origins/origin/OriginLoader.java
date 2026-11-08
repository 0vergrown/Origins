package dev.overgrown.origins.origin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
import dev.overgrown.apoli.loader.LoadProblems;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OriginLoader extends SimplePreparableReloadListener<Map<ResourceLocation, JsonDataFiles.Version>> {
    private static final String DIR = "origins";

    public OriginLoader() {
        super();
    }

    @Override
    protected @NotNull Map<ResourceLocation, JsonDataFiles.Version> prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        return JsonDataFiles.loadByPriority(resourceManager, DIR, Origins.LOGGER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonDataFiles.Version> entries, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        List<Origin> loaded = new ArrayList<>(entries.size());
        for (Map.Entry<ResourceLocation, JsonDataFiles.Version> entry : entries.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement value = entry.getValue().json();
            JsonDataFiles.Source source = entry.getValue().source();
            try {
                if (!value.isJsonObject()) {
                    LoadProblems.error("origin", id, source, "The file isn't a JSON object, so it was skipped.");
                    continue;
                }
                if (!ApoliReloadListener.loadConditionPasses(
                    new com.mojang.serialization.Dynamic<>(JsonOps.INSTANCE, value), id, source)) continue;
                JsonObject json = (JsonObject) IdWildcards.apply(value, id);
                LoadProblems.parse("origin", id, source, "$", Origin.codec(id).codec(),
                        new com.mojang.serialization.Dynamic<>(JsonOps.INSTANCE, (JsonElement) json))
                    .ifPresent(loaded::add);
            } catch (Exception e) {
                LoadProblems.error("origin", id, source, "Couldn't load this origin: " + e);
            }
        }
        OriginRegistry.replaceAll(loaded);
        int withUpgrades = 0;
        for (Origin origin : loaded) {
            if (!origin.upgrades().isEmpty()) withUpgrades++;
        }
        if (withUpgrades > 0) {
            Origins.LOGGER.info("Loaded {} origins, {} of them with upgrades.", OriginRegistry.size(), withUpgrades);
        } else {
            Origins.LOGGER.info("Loaded {} origins.", OriginRegistry.size());
        }
    }
}
