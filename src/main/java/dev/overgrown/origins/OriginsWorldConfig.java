package dev.overgrown.origins;

import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.origins.origin.ConditionedOrigin;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginPowerEntry;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OriginsWorldConfig {
    private static final String FILE = "origins/config.json";
    private static final String LEGACY_FILE = "origins/layers.json";

    private record Toggle(boolean enabled, Map<ResourceLocation, Boolean> entries) {
        static Codec<Toggle> codec(String entriesKey) {
            return RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("enabled").xmap(value -> value.orElse(true), Optional::of)
                    .forGetter(Toggle::enabled),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).optionalFieldOf(entriesKey)
                    .xmap(value -> value.orElse(Map.of()), Optional::of).forGetter(Toggle::entries)
            ).apply(i, Toggle::new));
        }

        boolean allows(ResourceLocation id) {
            return !Boolean.FALSE.equals(entries.get(id));
        }
    }

    private record Data(Map<ResourceLocation, Toggle> layers, Map<ResourceLocation, Toggle> origins) {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Toggle.codec("origins"))
                .optionalFieldOf("layers", Map.of()).forGetter(Data::layers),
            Codec.unboundedMap(ResourceLocation.CODEC, Toggle.codec("powers"))
                .optionalFieldOf("origins", Map.of()).forGetter(Data::origins)
        ).apply(i, Data::new));
    }

    private static volatile @Nullable Data active;

    private OriginsWorldConfig() {}

    public static void apply(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path path = root.resolve(FILE);
        Path source = Files.exists(path) ? path : root.resolve(LEGACY_FILE);
        Data stored = null;
        boolean writable = true;
        if (Files.exists(source)) {
            try {
                DataResult<Data> result = Data.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(Files.readString(source)));
                stored = result.resultOrPartial(error ->
                    Origins.LOGGER.warn("[Origins] Invalid {}: {}", source, error)).orElse(null);
                writable = result.result().isPresent();
            } catch (Exception e) {
                Origins.LOGGER.warn("[Origins] Couldn't read {} ({}); it is left untouched and nothing is disabled.",
                    source, e.toString());
                writable = false;
            }
        }
        Data data = complete(stored);
        if (writable) write(path, data);
        active = data;
        OriginRegistry.refilter();
        OriginLayers.refilter();
    }

    public static void clear() {
        active = null;
    }

    public static boolean blocks(ResourceLocation layer, ResourceLocation origin) {
        Data data = active;
        if (data == null) return false;
        Toggle layerToggle = data.layers().get(layer);
        if (layerToggle != null && (!layerToggle.enabled() || !layerToggle.allows(origin))) return true;
        Toggle originToggle = data.origins().get(origin);
        return originToggle != null && !originToggle.enabled();
    }

    public static @Nullable Origin filter(Origin origin) {
        Data data = active;
        if (data == null) return origin;
        Toggle toggle = data.origins().get(origin.id());
        if (toggle == null) return origin;
        if (!toggle.enabled()) return null;
        boolean changed = false;
        List<OriginPowerEntry> entries = new ArrayList<>(origin.powerEntries().size());
        for (OriginPowerEntry entry : origin.powerEntries()) {
            List<ResourceLocation> powers = new ArrayList<>(entry.powers().size());
            for (ResourceLocation power : entry.powers()) {
                if (toggle.allows(power)) powers.add(power);
                else changed = true;
            }
            entries.add(new OriginPowerEntry(entry.condition(), powers));
        }
        return changed ? origin.withPowerEntries(entries) : origin;
    }

    public static @Nullable OriginLayer filter(OriginLayer layer) {
        Data data = active;
        if (data == null) return layer;
        Toggle toggle = data.layers().get(layer.id());
        if (toggle != null && !toggle.enabled()) return null;
        boolean changed = false;
        List<ConditionedOrigin> entries = new ArrayList<>(layer.conditionedOrigins().size());
        for (ConditionedOrigin entry : layer.conditionedOrigins()) {
            List<ResourceLocation> origins = new ArrayList<>(entry.origins().size());
            for (ResourceLocation origin : entry.origins()) {
                Toggle originToggle = data.origins().get(origin);
                if ((toggle == null || toggle.allows(origin)) && (originToggle == null || originToggle.enabled())) {
                    origins.add(origin);
                } else {
                    changed = true;
                }
            }
            entries.add(new ConditionedOrigin(entry.condition(), origins));
        }
        return changed ? layer.withConditionedOrigins(entries) : layer;
    }

    private static Data complete(@Nullable Data stored) {
        Map<ResourceLocation, Toggle> layers = new HashMap<>();
        for (OriginLayer layer : OriginLayers.loaded()) {
            layers.put(layer.id(), merge(stored == null ? null : stored.layers().get(layer.id()), layer.allOrigins()));
        }
        Map<ResourceLocation, Toggle> origins = new HashMap<>();
        for (Origin origin : OriginRegistry.loaded()) {
            if (origin.id().equals(OriginRegistry.EMPTY_ID)) continue;
            origins.put(origin.id(), merge(stored == null ? null : stored.origins().get(origin.id()), origin.powers()));
        }
        if (stored != null) {
            keepDisabled(stored.layers(), layers);
            keepDisabled(stored.origins(), origins);
        }
        return new Data(layers, origins);
    }

    private static Toggle merge(@Nullable Toggle stored, List<ResourceLocation> ids) {
        Map<ResourceLocation, Boolean> entries = new HashMap<>();
        if (stored != null) {
            for (Map.Entry<ResourceLocation, Boolean> entry : stored.entries().entrySet()) {
                if (!entry.getValue()) entries.put(entry.getKey(), false);
            }
        }
        for (ResourceLocation id : ids) entries.putIfAbsent(id, true);
        return new Toggle(stored == null || stored.enabled(), entries);
    }

    private static void keepDisabled(Map<ResourceLocation, Toggle> stored, Map<ResourceLocation, Toggle> out) {
        for (Map.Entry<ResourceLocation, Toggle> entry : stored.entrySet()) {
            if (out.containsKey(entry.getKey())) continue;
            Toggle kept = merge(entry.getValue(), List.of());
            if (!kept.enabled() || !kept.entries().isEmpty()) out.put(entry.getKey(), kept);
        }
    }

    private static void write(Path path, Data data) {
        Data.CODEC.encodeStart(JsonOps.INSTANCE, data)
            .resultOrPartial(error -> Origins.LOGGER.warn("[Origins] Couldn't encode {}: {}", path, error))
            .ifPresent(json -> {
                try {
                    StringWriter text = new StringWriter();
                    JsonWriter writer = new JsonWriter(text);
                    writer.setIndent("  ");
                    GsonHelper.writeValue(writer, json, Comparator.naturalOrder());
                    String content = text.toString();
                    if (Files.exists(path) && content.equals(Files.readString(path))) return;
                    Files.createDirectories(path.getParent());
                    Files.writeString(path, content);
                } catch (Exception e) {
                    Origins.LOGGER.warn("[Origins] Couldn't write {} ({}).", path, e.toString());
                }
            });
    }
}
