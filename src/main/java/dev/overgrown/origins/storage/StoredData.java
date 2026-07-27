package dev.overgrown.origins.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class StoredData {
    public record StoredOrigin(ResourceLocation layer, ResourceLocation origin) {
        public static final Codec<StoredOrigin> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("layer").forGetter(StoredOrigin::layer),
            ResourceLocation.CODEC.fieldOf("origin").forGetter(StoredOrigin::origin)
        ).apply(i, StoredOrigin::new));
    }

    private final Map<String, StoredOrigin> origins;
    private final Map<String, String> values;

    public StoredData() {
        this(Map.of(), Map.of());
    }

    public StoredData(Map<String, StoredOrigin> origins, Map<String, String> values) {
        this.origins = new HashMap<>(origins);
        this.values = new HashMap<>(values);
    }

    public @Nullable StoredOrigin getOrigin(String key) {
        return origins.get(key);
    }

    public void setOrigin(String key, StoredOrigin value) {
        origins.put(key, value);
    }

    public boolean removeOrigin(String key) {
        return origins.remove(key) != null;
    }

    public @Nullable String getValue(String key) {
        return values.get(key);
    }

    public void setValue(String key, String value) {
        values.put(key, value);
    }

    public boolean removeValue(String key) {
        return values.remove(key) != null;
    }

    public boolean has(String key) {
        return origins.containsKey(key) || values.containsKey(key);
    }

    public boolean isEmpty() {
        return origins.isEmpty() && values.isEmpty();
    }

    public void clear() {
        origins.clear();
        values.clear();
    }

    public Set<String> keys() {
        Set<String> out = new java.util.HashSet<>(origins.keySet());
        out.addAll(values.keySet());
        return out;
    }

    public Map<String, StoredOrigin> originsView() {
        return Map.copyOf(origins);
    }

    public Map<String, String> valuesView() {
        return Map.copyOf(values);
    }

    public static final Codec<StoredData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, StoredOrigin.CODEC).optionalFieldOf("origins", Map.of())
            .forGetter(StoredData::originsView),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of())
            .forGetter(StoredData::valuesView)
    ).apply(i, StoredData::new));
}
