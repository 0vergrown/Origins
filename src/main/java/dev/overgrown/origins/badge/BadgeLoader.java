package dev.overgrown.origins.badge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class BadgeLoader extends SimplePreparableReloadListener<BadgeLoader.Prepared> {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String BADGES_DIR = "badges";
    private static final String POWERS_DIR = "powers";

    public record Prepared(Map<ResourceLocation, Badge> standalone, Map<ResourceLocation, List<Badge>> byPower) {}

    @Override
    protected Prepared prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<ResourceLocation, Badge> standalone = new LinkedHashMap<>();
        Map<ResourceLocation, List<Badge>> byPower = new LinkedHashMap<>();

        rm.listResources(BADGES_DIR, loc -> loc.getPath().endsWith(".json")).forEach((loc, resource) -> {
            ResourceLocation badgeId = trim(loc, BADGES_DIR);
            JsonElement json = read(resource, loc);
            if (json == null) return;
            Badge.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(err -> Origins.LOGGER.error("Failed to read badge {}: {}", badgeId, err))
                .ifPresent(badge -> standalone.put(badgeId, badge));
        });

        rm.listResources(POWERS_DIR, loc -> loc.getPath().endsWith(".json")).forEach((loc, resource) -> {
            ResourceLocation powerId = trim(loc, POWERS_DIR);
            JsonElement json = read(resource, loc);
            if (!(json instanceof JsonObject obj) || !obj.has(BADGES_DIR)) return;
            if (obj.has("hidden") && obj.get("hidden").getAsBoolean()) return;
            if (!(obj.get(BADGES_DIR) instanceof JsonArray array)) return;

            List<Badge> badges = new LinkedList<>();
            for (JsonElement element : array) {
                Badge badge = readBadge(element, standalone, powerId);
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

    private static Badge readBadge(JsonElement element, Map<ResourceLocation, Badge> standalone, ResourceLocation powerId) {
        if (element instanceof JsonObject object) {
            if (!object.has("type")) object.addProperty("type", BadgeTypes.DEFAULT.toString());
            return Badge.CODEC.parse(JsonOps.INSTANCE, object)
                .resultOrPartial(err -> Origins.LOGGER.error("Bad inline badge on power {}: {}", powerId, err))
                .orElse(null);
        }
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            ResourceLocation ref = ResourceLocation.tryParse(primitive.getAsString());
            Badge referenced = ref == null ? null : standalone.get(ref);
            if (referenced == null) {
                Origins.LOGGER.error("Power {} references undefined badge {}", powerId, primitive.getAsString());
            }
            return referenced;
        }
        return null;
    }

    private static ResourceLocation trim(ResourceLocation loc, String dir) {
        String path = loc.getPath();
        path = path.substring(dir.length() + 1, path.length() - ".json".length());
        return new ResourceLocation(loc.getNamespace(), path);
    }

    private static JsonElement read(Resource resource, ResourceLocation loc) {
        try (Reader reader = resource.openAsReader()) {
            return GSON.fromJson(reader, JsonElement.class);
        } catch (Exception e) {
            Origins.LOGGER.error("Failed to read badge resource {}: {}", loc, e.toString());
            return null;
        }
    }
}
