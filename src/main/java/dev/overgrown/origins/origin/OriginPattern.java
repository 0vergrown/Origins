package dev.overgrown.origins.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class OriginPattern {
    private static final char WILDCARD = '*';

    private final String raw;
    private final @Nullable ResourceLocation exact;
    private final String namespace;
    private final String path;

    private OriginPattern(String raw, @Nullable ResourceLocation exact, String namespace, String path) {
        this.raw = raw;
        this.exact = exact;
        this.namespace = namespace;
        this.path = path;
    }

    public static final Codec<OriginPattern> CODEC = Codec.STRING.comapFlatMap(
        OriginPattern::parse, OriginPattern::raw);

    public static DataResult<OriginPattern> parse(String raw) {
        if (raw.indexOf(WILDCARD) < 0) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            return id == null
                ? DataResult.error(() -> "Not a valid origin id: " + raw)
                : DataResult.success(new OriginPattern(raw, id, id.getNamespace(), id.getPath()));
        }
        int colon = raw.indexOf(':');
        String namespace = colon < 0 ? String.valueOf(WILDCARD) : raw.substring(0, colon);
        String path = colon < 0 ? raw : raw.substring(colon + 1);
        if (namespace.isEmpty() || path.isEmpty()) {
            return DataResult.error(() -> "Not a valid origin pattern: " + raw);
        }
        return DataResult.success(new OriginPattern(raw, null, namespace, path));
    }

    public String raw() {
        return raw;
    }

    public boolean matches(ResourceLocation id) {
        if (exact != null) return exact.equals(id);
        return glob(namespace, id.getNamespace()) && glob(path, id.getPath());
    }

    private static boolean glob(String pattern, String value) {
        int p = 0;
        int v = 0;
        int star = -1;
        int mark = 0;
        while (v < value.length()) {
            if (p < pattern.length() && pattern.charAt(p) == WILDCARD) {
                star = p++;
                mark = v;
            } else if (p < pattern.length() && pattern.charAt(p) == value.charAt(v)) {
                p++;
                v++;
            } else if (star >= 0) {
                p = star + 1;
                v = ++mark;
            } else {
                return false;
            }
        }
        while (p < pattern.length() && pattern.charAt(p) == WILDCARD) p++;
        return p == pattern.length();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof OriginPattern other && raw.equals(other.raw));
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return raw;
    }
}
