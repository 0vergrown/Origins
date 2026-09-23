package dev.overgrown.origins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.origins.origin.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public record OriginsWorldConfig (
    HashMap<ResourceLocation, LayerConfig> layers,
    HashMap<ResourceLocation, OriginConfig> origins
) {
    public static MinecraftServer server;

    public static void attachServer(MinecraftServer minecraftServer)  {
        server = minecraftServer;

        OriginsWorldConfig.get(server);
    }

    public HashMap<ResourceLocation, LayerConfig> nonDefaultLayers(HashMap<ResourceLocation, LayerConfig> original) {
        HashMap<ResourceLocation, LayerConfig> result = new HashMap<>();

        for (var entry : original.entrySet()) {
            if (!entry.getValue().enabled()) {
                result.put(entry.getKey(), entry.getValue());
                continue;
            }

            for (var origin : entry.getValue().origins.entrySet()) {
                if (!origin.getValue()) {
                    result.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        return result;
    }

    public HashMap<ResourceLocation, OriginConfig> nonDefaultOrigins(HashMap<ResourceLocation, OriginConfig> original) {
        HashMap<ResourceLocation, OriginConfig> result = new HashMap<>();

        for (var entry : original.entrySet()) {
            if (!entry.getValue().enabled()) {
                result.put(entry.getKey(), entry.getValue());
                continue;
            }

            for (var power : entry.getValue().powers.entrySet()) {
                if (!power.getValue()) {
                    result.put(entry.getKey(), entry.getValue());
                    break;
                }
            }
        }

        return result;
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FILE = "layers.json";

    private static OriginsWorldConfig defaultCfg() {
        var result = new OriginsWorldConfig(new HashMap<>(), new HashMap<>());

        for (OriginLayer layer : OriginLayers.all()) {
            HashMap<ResourceLocation, Boolean> origins = new HashMap<>();

            for (ResourceLocation originID : layer.allOrigins()) {
                origins.put(originID, true);
            }

            result.layers.put(layer.id(), new LayerConfig(true, origins));
        }

        for (Origin origin : OriginRegistry.all()) {
            if(Objects.equals(origin.id(), new ResourceLocation("origins:empty"))) continue;

            HashMap<ResourceLocation, Boolean> powers = new HashMap<>();

            for (ResourceLocation power : origin.powers()) {
                powers.put(power, true);
            }

            result.origins.put(origin.id(), new OriginConfig(true, powers));
        }

        return result;
    }

    private OriginsWorldConfig filled()  {
        var result = new OriginsWorldConfig(nonDefaultLayers(layers), nonDefaultOrigins(origins));
        var defaultCfg = defaultCfg();

        for (OriginLayer layer : OriginLayers.all()) {
            HashMap<ResourceLocation, Boolean> origins = layers.getOrDefault(layer.id(), defaultCfg.layers.get(layer.id())).origins;

            for (ResourceLocation originID : layer.allOrigins()) {
                origins.putIfAbsent(originID, true);
            }

            result.layers.put(layer.id(), new LayerConfig(layerEnabled(layer.id()), origins));
        }

        for (Origin origin : OriginRegistry.all()) {
            if(Objects.equals(origin.id(), new ResourceLocation("origins:empty"))) continue;

            HashMap<ResourceLocation, Boolean> powers = origins.getOrDefault(origin.id(), defaultCfg.origins.get(origin.id())).powers;

            for (ResourceLocation power : origin.powers()) {
                powers.putIfAbsent(power, true);
            }

            result.origins.put(origin.id(), new OriginConfig(originEnabled(origin.id()), powers));
        }

        return result;
    }

    public static final Codec<OriginsWorldConfig> CODEC = RecordCodecBuilder.create((i) -> i.group(
            Codec.unboundedMap(ResourceLocation.CODEC, LayerConfig.CODEC).fieldOf("layers").forGetter(OriginsWorldConfig::layers),
            Codec.unboundedMap(ResourceLocation.CODEC, OriginConfig.CODEC).fieldOf("origins").forGetter(OriginsWorldConfig::origins)
    ).apply(i, (layers, origins) ->
        new OriginsWorldConfig(new HashMap<>(layers), new HashMap<>(origins))
    ));

    private static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("origins/" + FILE);
    }

    public record OriginConfig(
        boolean enabled,
        HashMap<ResourceLocation, Boolean> powers
    ) {
        public boolean powerEnabled(ResourceLocation id) {
            return !powers.containsKey(id) || powers.get(id);
        }

        public static Codec<OriginConfig> CODEC = RecordCodecBuilder.create((i) -> i.group(
                Codec.BOOL.fieldOf("enabled").forGetter(OriginConfig::enabled),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).fieldOf("powers").forGetter(OriginConfig::powers)
        ).apply(i, (bl, map) -> new OriginConfig(bl, new HashMap<>(map))));
    }

    public record LayerConfig(
        boolean enabled,
        HashMap<ResourceLocation, Boolean> origins

    ) {
        public static Codec<LayerConfig> CODEC = RecordCodecBuilder.create((i) -> i.group(
                Codec.BOOL.fieldOf("enabled").forGetter(LayerConfig::enabled),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).fieldOf("origins").forGetter(LayerConfig::origins)
        ).apply(i, (bl, map) -> new LayerConfig(bl, new HashMap<>(map))));
    }

    public LayerConfig layer(ResourceLocation layer) {
        return layers.get(layer);
    }

    public static OriginsWorldConfig get(@Nullable MinecraftServer server) {
        if (server == null) return defaultCfg();

        Path path = path(server);

        if (!Files.exists(path)) {
            var config = defaultCfg();
            save(path, config);
            return config;
        }

        try (var reader = Files.newBufferedReader(path)) {
            var json = JsonParser.parseReader(reader);

            var res = CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error ->
                            Apoli.LOGGER.error("Failed to load world config at {}: {}", path, error)
                    )
                    .orElseGet(OriginsWorldConfig::defaultCfg);

            res = res.filled();

            save(path, res);

            return res;
        } catch (IOException e) {
            Apoli.LOGGER.error("Failed to load world config at {}: {}", path, e);
            return defaultCfg();
        }
    }

    private static void save(Path path, OriginsWorldConfig config) {
        try {
            Files.createDirectories(path.getParent());

            var json = CODEC.encodeStart(JsonOps.INSTANCE, config)
                    .getOrThrow(false, (e) -> Apoli.LOGGER.error("Failed to save world config at {}: {}", path, e));

            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            Apoli.LOGGER.error("Failed to save world config at {}: {}", path, e);
        }
    }

    public boolean layerEnabled(ResourceLocation layer) {
        return !layers.containsKey(layer) || layers.get(layer).enabled;
    }

    public boolean layerOriginEnabled(ResourceLocation layer, ResourceLocation origin) {
        return !layers.containsKey(layer) || !layers.get(layer).origins.containsKey(origin) || layers.get(layer).origins.get(origin);
    }

    public boolean originEnabled(ResourceLocation origin) {
        return !origins.containsKey(origin) || origins.get(origin).enabled;
    }

    public boolean powerEnabled(ResourceLocation origin, ResourceLocation power) {
        return !origins.containsKey(origin) || !origins.get(origin).powers.containsKey(power) || origins.get(origin).powers.get(power);
    }

    public List<ConditionedOrigin> filterLayerOrigins(List<ConditionedOrigin> original, ResourceLocation layer) {
        return original.stream().map(conditionedOrigin ->
            new ConditionedOrigin(conditionedOrigin.condition(), conditionedOrigin.origins().stream().filter(origin -> layerOriginEnabled(layer, origin)).toList())
        ).toList();
    }

    public List<OriginPowerEntry> filterPowers(List<OriginPowerEntry> original, ResourceLocation origin) {
        return original.stream().map(originPowerEntry ->
                new OriginPowerEntry(originPowerEntry.condition(), originPowerEntry.powers().stream().filter(power -> powerEnabled(origin, power)).toList())
        ).toList();
    }

    public Collection<OriginLayer> filterLayers(Collection<OriginLayer> original) {
        return original.stream().filter(layer -> layerEnabled(layer.id())).toList();
    }

    public Collection<Origin> filterOrigins(Collection<Origin> original) {
        return original.stream().filter(origin -> originEnabled(origin.id())).toList();
    }
}
