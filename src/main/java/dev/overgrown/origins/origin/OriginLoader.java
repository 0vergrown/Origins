package dev.overgrown.origins.origin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.ApoliReloadListener;
import dev.overgrown.apoli.loader.IdWildcards;
import dev.overgrown.apoli.loader.JsonDataFiles;
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
            try {
                if (!value.isJsonObject()) continue;
                if (!ApoliReloadListener.loadConditionPasses(
                    new com.mojang.serialization.Dynamic<>(JsonOps.INSTANCE, value), id)) continue;
                JsonObject json = (JsonObject) IdWildcards.apply(value, id);
                dev.overgrown.apoli.codec.LoggedOptionalField.setContext(id);
                try {
                    Origin.codec(id).codec().parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> Origins.LOGGER.error("Failed to load origin {}: {}", id, err))
                        .ifPresent(loaded::add);
                } finally {
                    dev.overgrown.apoli.codec.LoggedOptionalField.clearContext();
                }
            } catch (Exception e) {
                Origins.LOGGER.error("Failed to load origin {}: {}", id, e.getMessage());
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
