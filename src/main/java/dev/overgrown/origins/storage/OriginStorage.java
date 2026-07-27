package dev.overgrown.origins.storage;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.storage.StoredData.StoredOrigin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OriginStorage {
    public static final ResourceLocation DEFAULT_LAYER = Origins.id("origin");

    private static final Pattern PLACEHOLDER = Pattern.compile("\\[([^\\[\\]]+)]");

    private OriginStorage() {}

    public static boolean storeOrigin(Player holder, String key, Player source, ResourceLocation layerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(source);
        if (state == null || !state.hasOrigin(layerId)) return false;
        return storeOrigin(holder, key, layerId, state.getOrigin(layerId));
    }

    public static boolean storeOrigin(Player holder, String key, ResourceLocation layerId, ResourceLocation originId) {
        if (originId == null || originId.equals(OriginRegistry.EMPTY_ID)) return false;
        StoredDataAttachment.getOrCreate(holder).setOrigin(key, new StoredOrigin(layerId, originId));
        return true;
    }

    public static boolean applyStoredOrigin(ServerPlayer holder, String key,
                                            @Nullable ResourceLocation layerOverride, boolean clear) {
        StoredData data = StoredDataAttachment.get(holder);
        if (data == null) return false;
        StoredOrigin stored = data.getOrigin(key);
        if (stored == null) return false;
        ResourceLocation layerId = layerOverride != null ? layerOverride : stored.layer();
        if (OriginLayers.get(layerId) == null || OriginRegistry.get(stored.origin()) == null) return false;
        OriginManager.chooseOrigin(holder, layerId, stored.origin(), false);
        MinecraftServer server = holder.getServer();
        if (server != null) OriginsServerNetwork.broadcastPlayerOrigins(server, holder);
        if (clear) data.removeOrigin(key);
        return true;
    }

    public static void storeValue(Player holder, String key, String value) {
        StoredDataAttachment.getOrCreate(holder).setValue(key, value);
    }

    public static boolean clear(Player holder, @Nullable String key) {
        StoredData data = StoredDataAttachment.get(holder);
        if (data == null) return false;
        if (key == null) {
            if (data.isEmpty()) return false;
            data.clear();
            return true;
        }
        boolean removedOrigin = data.removeOrigin(key);
        boolean removedValue = data.removeValue(key);
        return removedOrigin || removedValue;
    }

    public static @Nullable StoredOrigin origin(Player holder, String key) {
        StoredData data = StoredDataAttachment.get(holder);
        return data == null ? null : data.getOrigin(key);
    }

    public static @Nullable String value(Player holder, String key) {
        StoredData data = StoredDataAttachment.get(holder);
        return data == null ? null : data.getValue(key);
    }

    public static boolean has(Player holder, String key) {
        StoredData data = StoredDataAttachment.get(holder);
        return data != null && data.has(key);
    }

    public static @Nullable String lookup(Player holder, String key) {
        StoredData data = StoredDataAttachment.get(holder);
        if (data == null) return null;
        boolean wantsName = key.endsWith(".name");
        String baseKey = wantsName ? key.substring(0, key.length() - ".name".length()) : key;
        StoredOrigin stored = data.getOrigin(baseKey);
        if (stored != null) {
            if (!wantsName) return stored.origin().toString();
            Origin origin = OriginRegistry.get(stored.origin());
            return origin == null ? null : origin.name().getString();
        }
        return wantsName ? null : data.getValue(baseKey);
    }

    public static @Nullable String resolve(Player holder, String template) {
        if (template.indexOf('[') < 0) return template;
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = lookup(holder, matcher.group(1));
            if (replacement == null) return null;
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
