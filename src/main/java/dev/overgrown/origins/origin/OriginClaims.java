package dev.overgrown.origins.origin;

import com.mojang.serialization.Codec;
import dev.overgrown.origins.Origins;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class OriginClaims extends SavedData {

    public static final String FILE_ID = "origins_claims";

    private static final Codec<Set<UUID>> HOLDERS = UUIDUtil.CODEC.listOf()
        .<Set<UUID>>xmap(LinkedHashSet::new, List::copyOf);

    private static final Codec<Map<ResourceLocation, Map<ResourceLocation, Set<UUID>>>> CODEC =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.unboundedMap(ResourceLocation.CODEC, HOLDERS));

    private final Map<ResourceLocation, Map<ResourceLocation, Set<UUID>>> byLayer = new HashMap<>();

    public static OriginClaims get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OriginClaims::load, OriginClaims::new, FILE_ID);
    }

    public static OriginClaims load(CompoundTag tag) {
        OriginClaims claims = new OriginClaims();
        Tag stored = tag.get("claims");
        if (stored == null) return claims;
        CODEC.parse(NbtOps.INSTANCE, stored)
            .resultOrPartial(error -> Origins.LOGGER.error("[Origins] Could not read the origin claim ledger: {}", error))
            .ifPresent(map -> map.forEach((layerId, origins) -> origins.forEach((originId, holders) -> {
                if (holders.isEmpty()) return;
                claims.byLayer.computeIfAbsent(layerId, key -> new HashMap<>())
                    .computeIfAbsent(originId, key -> new LinkedHashSet<>())
                    .addAll(holders);
            })));
        return claims;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CODEC.encodeStart(NbtOps.INSTANCE, byLayer)
            .resultOrPartial(error -> Origins.LOGGER.error("[Origins] Could not write the origin claim ledger: {}", error))
            .ifPresent(encoded -> tag.put("claims", encoded));
        return tag;
    }

    public boolean refresh(UUID player, Map<ResourceLocation, Set<ResourceLocation>> held) {
        boolean changed = false;
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, Set<UUID>>> layerEntry : byLayer.entrySet()) {
            Set<ResourceLocation> keep = held.getOrDefault(layerEntry.getKey(), Set.of());
            Iterator<Map.Entry<ResourceLocation, Set<UUID>>> iterator = layerEntry.getValue().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ResourceLocation, Set<UUID>> originEntry = iterator.next();
                if (keep.contains(originEntry.getKey())) continue;
                if (originEntry.getValue().remove(player)) changed = true;
                if (originEntry.getValue().isEmpty()) iterator.remove();
            }
        }
        for (Map.Entry<ResourceLocation, Set<ResourceLocation>> layerEntry : held.entrySet()) {
            Map<ResourceLocation, Set<UUID>> origins =
                byLayer.computeIfAbsent(layerEntry.getKey(), key -> new HashMap<>());
            for (ResourceLocation originId : layerEntry.getValue()) {
                if (origins.computeIfAbsent(originId, key -> new LinkedHashSet<>()).add(player)) changed = true;
            }
        }
        byLayer.values().removeIf(Map::isEmpty);
        if (changed) setDirty();
        return changed;
    }

    public Set<UUID> holders(ResourceLocation layerId, ResourceLocation originId) {
        Map<ResourceLocation, Set<UUID>> origins = byLayer.get(layerId);
        if (origins == null) return Set.of();
        Set<UUID> holders = origins.get(originId);
        return holders == null ? Set.of() : Set.copyOf(holders);
    }

    public boolean clearAll() {
        if (byLayer.isEmpty()) return false;
        byLayer.clear();
        setDirty();
        return true;
    }

    public boolean clearLayer(ResourceLocation layerId) {
        if (byLayer.remove(layerId) == null) return false;
        setDirty();
        return true;
    }

    public boolean clearOrigin(ResourceLocation layerId, ResourceLocation originId) {
        Map<ResourceLocation, Set<UUID>> origins = byLayer.get(layerId);
        if (origins == null || origins.remove(originId) == null) return false;
        if (origins.isEmpty()) byLayer.remove(layerId);
        setDirty();
        return true;
    }

    public Map<ResourceLocation, Map<ResourceLocation, Integer>> counts() {
        Map<ResourceLocation, Map<ResourceLocation, Integer>> out = new HashMap<>(byLayer.size());
        byLayer.forEach((layerId, origins) -> {
            Map<ResourceLocation, Integer> inner = new HashMap<>(origins.size());
            origins.forEach((originId, holders) -> {
                if (!holders.isEmpty()) inner.put(originId, holders.size());
            });
            if (!inner.isEmpty()) out.put(layerId, inner);
        });
        return out;
    }

    public Map<ResourceLocation, Map<ResourceLocation, Set<UUID>>> snapshot() {
        Map<ResourceLocation, Map<ResourceLocation, Set<UUID>>> out = new HashMap<>(byLayer.size());
        byLayer.forEach((layerId, origins) -> {
            Map<ResourceLocation, Set<UUID>> inner = new HashMap<>(origins.size());
            origins.forEach((originId, holders) -> inner.put(originId, Set.copyOf(holders)));
            out.put(layerId, inner);
        });
        return out;
    }
}
