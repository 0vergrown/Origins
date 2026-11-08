package dev.overgrown.origins.badge;

import com.google.gson.JsonElement;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.loader.JsonDataFiles;
import dev.overgrown.apoli.loader.LoadProblems;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.NOPLogger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class BadgeLoader extends SimplePreparableReloadListener<BadgeLoader.Prepared> {

    private static final String BADGES_DIR = "badges";
    private static final String POWERS_DIR = "powers";

    public record Prepared(Map<ResourceLocation, Badge> standalone, Map<ResourceLocation, List<Badge>> byPower) {}

    @Override
    protected @NotNull Prepared prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Badge> standalone = new LinkedHashMap<>();
        Map<ResourceLocation, List<Badge>> byPower = new LinkedHashMap<>();

        Map<ResourceLocation, JsonDataFiles.Source> badgeFiles = new java.util.HashMap<>();
        JsonDataFiles.load(rm, BADGES_DIR, Origins.LOGGER, badgeFiles).forEach((badgeId, json) ->
            LoadProblems.parse("badge", badgeId, badgeFiles.get(badgeId), "$", Badge.CODEC, new Dynamic<>(JsonOps.INSTANCE, json))
                .ifPresent(badge -> standalone.put(badgeId, badge)));

        JsonDataFiles.topSources(rm, POWERS_DIR, NOPLogger.NOP_LOGGER).forEach((powerId, source) -> {
            JsonElement json = JsonDataFiles.read(source, powerId, NOPLogger.NOP_LOGGER, BADGES_DIR);
            if (json == null) return;
            Dynamic<JsonElement> power = new Dynamic<>(JsonOps.INSTANCE, json);
            if (power.get("hidden").asBoolean(false)) return;
            List<Dynamic<JsonElement>> entries = power.get(BADGES_DIR).asList(Function.identity());
            if (entries.isEmpty()) return;
            List<Badge> badges = new ArrayList<>(entries.size());
            for (int i = 0; i < entries.size(); i++) {
                Badge badge = readBadge(entries.get(i), standalone, powerId, source, "$." + BADGES_DIR + "[" + i + "]");
                if (badge != null) badges.add(badge);
            }
            if (!badges.isEmpty()) byPower.put(powerId, badges);
        });

        return new Prepared(standalone, byPower);
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager rm, ProfilerFiller profiler) {
        BadgeManager.clear();
        BadgeManager.STANDALONE.putAll(prepared.standalone());
        BadgeManager.BY_POWER.putAll(prepared.byPower());
        Origins.LOGGER.info("[Origins] Loaded {} standalone badge(s); {} power(s) carry inline badges.",
            prepared.standalone().size(), prepared.byPower().size());
    }

    private static Badge readBadge(Dynamic<JsonElement> entry, Map<ResourceLocation, Badge> standalone, ResourceLocation powerId,
                                   JsonDataFiles.Source source, String path) {
        if (entry.getMapValues().result().isPresent()) {
            Dynamic<JsonElement> typed = entry.get("type").result().isPresent()
                ? entry
                : entry.set("type", entry.createString(BadgeTypes.DEFAULT.toString()));
            return LoadProblems.parse("power badge", powerId, source, path, Badge.CODEC, typed).orElse(null);
        }
        String reference = entry.asString().result().orElse(null);
        if (reference == null) return null;
        ResourceLocation ref = ResourceLocation.tryParse(reference);
        Badge referenced = ref == null ? null : standalone.get(ref);
        if (referenced == null) {
            LoadProblems.warn("power badge", powerId, source, "Uses the badge " + reference + ", which doesn't exist, so the badge is left out", path);
        }
        return referenced;
    }
}
