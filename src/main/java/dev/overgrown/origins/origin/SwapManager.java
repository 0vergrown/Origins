package dev.overgrown.origins.origin;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.power.ActionOnSwapPower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SwapManager {

    private SwapManager() {}

    public static ResourceLocation sourceFor(ResourceLocation targetLayerId) {
        return ResourceLocation.fromNamespaceAndPath(targetLayerId.getNamespace(),
            "swap/" + targetLayerId.getPath());
    }

    public static @Nullable OriginLayer targetLayerOf(OriginLayer swapLayer) {
        ResourceLocation explicit = swapLayer.swap().targetLayer().orElse(null);
        if (explicit != null) return OriginLayers.get(explicit);
        for (OriginLayer candidate : OriginLayers.enabledOrdered()) {
            if (!candidate.swappable()) return candidate;
        }
        return null;
    }

    public static List<OriginLayer> swapLayersFor(ResourceLocation targetLayerId) {
        List<OriginLayer> out = new ArrayList<>(1);
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (!layer.swappable()) continue;
            OriginLayer target = targetLayerOf(layer);
            if (target != null && target.id().equals(targetLayerId)) out.add(layer);
        }
        return out;
    }

    public static @Nullable ResourceLocation resolveTarget(@Nullable ResourceLocation layerId) {
        if (layerId == null) {
            OriginLayer fallback = defaultTargetLayer();
            return fallback == null ? null : fallback.id();
        }
        OriginLayer named = OriginLayers.get(layerId);
        if (named == null) return null;
        if (!named.swappable()) return named.id();
        OriginLayer target = targetLayerOf(named);
        return target == null ? null : target.id();
    }

    public static @Nullable OriginLayer defaultTargetLayer() {
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.swappable()) continue;
            if (!swapLayersFor(layer.id()).isEmpty()) return layer;
        }
        return null;
    }

    public static List<ResourceLocation> pool(Player player, ResourceLocation targetLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return pool(player, targetLayerId, state == null ? null : state.getOrigin(targetLayerId));
    }

    public static List<ResourceLocation> pool(Player player, ResourceLocation targetLayerId,
                                              @Nullable ResourceLocation main) {
        return pool(player, targetLayerId, main, granted(player));
    }

    public static List<ResourceLocation> pool(Player player, ResourceLocation targetLayerId,
                                              @Nullable ResourceLocation main,
                                              Map<ResourceLocation, ? extends Collection<ResourceLocation>> granted) {
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        for (OriginLayer swapLayer : swapLayersFor(targetLayerId)) {
            for (ResourceLocation id : swapLayer.availableOrigins(player)) {
                if (id.equals(main) || id.equals(OriginRegistry.EMPTY_ID)) continue;
                if (OriginRegistry.get(id) == null) continue;
                seen.add(id);
            }
            Collection<ResourceLocation> explicit = granted.get(swapLayer.id());
            if (explicit == null) continue;
            for (ResourceLocation id : explicit) {
                if (id.equals(main) || id.equals(OriginRegistry.EMPTY_ID)) continue;
                if (OriginRegistry.get(id) == null) continue;
                seen.add(id);
            }
        }
        List<ResourceLocation> out = new ArrayList<>(seen);
        out.sort(Comparator.comparing(SwapManager::sortKey).thenComparing(ResourceLocation::toString));
        return out;
    }

    private static Map<ResourceLocation, ? extends Collection<ResourceLocation>> granted(Player player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? Map.of() : state.poolSnapshot();
    }

    public static boolean grantToPool(ServerPlayer player, ResourceLocation swapLayerId, ResourceLocation originId) {
        OriginLayer swapLayer = OriginLayers.get(swapLayerId);
        if (swapLayer == null || !swapLayer.swappable()) return false;
        if (OriginRegistry.get(originId) == null || originId.equals(OriginRegistry.EMPTY_ID)) return false;
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        if (!state.grantToPool(swapLayerId, originId)) return false;
        broadcast(player);
        return true;
    }

    public static boolean revokeFromPool(ServerPlayer player, ResourceLocation swapLayerId, ResourceLocation originId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        if (!state.revokeFromPool(swapLayerId, originId)) return false;
        revalidate(player);
        broadcast(player);
        return true;
    }

    public static void clearPool(ServerPlayer player, ResourceLocation swapLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || state.poolOf(swapLayerId).isEmpty()) return;
        state.clearPool(swapLayerId);
        revalidate(player);
        broadcast(player);
    }

    private static void broadcast(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) dev.overgrown.origins.network.OriginsServerNetwork.broadcastPlayerSwaps(server, player);
    }

    private static String sortKey(ResourceLocation originId) {
        Origin origin = OriginRegistry.get(originId);
        return origin == null ? originId.getPath() : origin.name().getString();
    }

    public static boolean hasPool(Player player, ResourceLocation targetLayerId) {
        return !pool(player, targetLayerId).isEmpty();
    }

    public static @Nullable ResourceLocation activeSwap(Player player, ResourceLocation targetLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? null : state.getActiveSwap(targetLayerId);
    }

    public static ResourceLocation activeOrigin(Player player, ResourceLocation targetLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        ResourceLocation swapped = state.getActiveSwap(targetLayerId);
        return swapped != null ? swapped : state.getOrigin(targetLayerId);
    }

    public static boolean applySwap(ServerPlayer player, ResourceLocation targetLayerId,
                                    @Nullable ResourceLocation incomingId) {
        OriginLayer targetLayer = OriginLayers.get(targetLayerId);
        if (targetLayer == null) return false;

        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        ResourceLocation mainId = state.getOrigin(targetLayerId);
        ResourceLocation outgoingId = state.getActiveSwap(targetLayerId);
        if (outgoingId == null) outgoingId = mainId;

        if (incomingId != null && incomingId.equals(mainId)) incomingId = null;
        ResourceLocation resolvedIncoming = incomingId == null ? mainId : incomingId;
        if (resolvedIncoming == null || resolvedIncoming.equals(outgoingId)) return false;
        if (incomingId != null && !pool(player, targetLayerId).contains(incomingId)) return false;

        Origin incoming = OriginRegistry.get(resolvedIncoming);
        if (incoming == null) return false;

        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return false;
        ResourceLocation source = sourceFor(targetLayerId);

        List<ResourceLocation> incomingPowers = incoming.powers();
        Set<ResourceLocation> keep = new HashSet<>(incomingPowers);

        if (incomingId != null) {
            for (ResourceLocation power : incomingPowers) {
                if (!container.sourcesOf(power).contains(source)) container.addPower(power, source);
            }
        }
        container.unsuppressAll(incomingPowers, source);
        revokeStaleSwapGrants(container, source, incomingId == null ? Set.of() : keep);
        hideMainBehindSwap(container, source, incomingId == null ? null : OriginRegistry.get(mainId), keep);

        state.setActiveSwap(targetLayerId, incomingId);
        ActionOnSwapPower.fire(player, targetLayerId, outgoingId, resolvedIncoming);
        broadcast(player);
        return true;
    }

    public static boolean cycle(ServerPlayer player, ResourceLocation targetLayerId, boolean toMain) {
        if (toMain) return applySwap(player, targetLayerId, null);
        List<ResourceLocation> options = pool(player, targetLayerId);
        if (options.isEmpty()) return false;
        ResourceLocation current = activeSwap(player, targetLayerId);
        if (current == null) return applySwap(player, targetLayerId, options.get(0));
        int index = options.indexOf(current);
        if (index < 0) return applySwap(player, targetLayerId, options.get(0));
        if (index + 1 >= options.size()) return applySwap(player, targetLayerId, null);
        return applySwap(player, targetLayerId, options.get(index + 1));
    }

    public static boolean random(ServerPlayer player, ResourceLocation targetLayerId, boolean includeMain) {
        List<ResourceLocation> options = new ArrayList<>(pool(player, targetLayerId));
        ResourceLocation current = activeSwap(player, targetLayerId);
        options.remove(current);
        int bound = options.size() + (includeMain && current != null ? 1 : 0);
        if (bound <= 0) return false;
        int pick = player.getRandom().nextInt(bound);
        return applySwap(player, targetLayerId, pick < options.size() ? options.get(pick) : null);
    }

    public static void resetToMain(ServerPlayer player, ResourceLocation targetLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        if (state.getActiveSwap(targetLayerId) == null) return;
        applySwap(player, targetLayerId, null);
    }

    public static void revokeSwapGrants(ServerPlayer player, ResourceLocation targetLayerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        state.setActiveSwap(targetLayerId, null);
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) {
            ResourceLocation source = sourceFor(targetLayerId);
            container.unsuppressAllFromSource(source);
            container.removeAllFromSource(source);
        }
        broadcast(player);
    }

    private static void revokeStaleSwapGrants(PowerContainer container, ResourceLocation source,
                                              Set<ResourceLocation> keep) {
        for (ResourceLocation power : container.allPowers()) {
            if (keep.contains(power)) continue;
            if (!container.sourcesOf(power).contains(source)) continue;
            container.unsuppressPower(power, source);
            container.removePower(power, source);
        }
    }

    private static void hideMainBehindSwap(PowerContainer container, ResourceLocation source,
                                           @Nullable Origin main, Set<ResourceLocation> keep) {
        if (main == null) return;
        List<ResourceLocation> hide = new ArrayList<>(main.powers().size());
        for (ResourceLocation power : main.powers()) {
            if (!keep.contains(power)) hide.add(power);
        }
        container.suppressAll(hide, source);
    }

    public static void reapplySuppression(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;

        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.swappable() || swapLayersFor(layer.id()).isEmpty()) continue;

            ResourceLocation targetLayerId = layer.id();
            ResourceLocation source = sourceFor(targetLayerId);
            ResourceLocation activeId = state.getActiveSwap(targetLayerId);
            Origin active = activeId == null ? null : OriginRegistry.get(activeId);

            if (active == null) {
                if (activeId != null) state.setActiveSwap(targetLayerId, null);
                container.unsuppressAllFromSource(source);
                container.removeAllFromSource(source);
                continue;
            }

            List<ResourceLocation> activePowers = active.powers();
            for (ResourceLocation power : activePowers) {
                if (!container.sourcesOf(power).contains(source)) container.addPower(power, source);
            }
            container.unsuppressAll(activePowers, source);
            Set<ResourceLocation> keep = new HashSet<>(activePowers);
            revokeStaleSwapGrants(container, source, keep);
            hideMainBehindSwap(container, source, OriginRegistry.get(state.getOrigin(targetLayerId)), keep);
        }
    }

    public static void revalidate(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        for (ResourceLocation targetLayerId : List.copyOf(state.swapSnapshot().keySet())) {
            ResourceLocation active = state.getActiveSwap(targetLayerId);
            if (active == null) continue;
            if (OriginLayers.get(targetLayerId) == null || !pool(player, targetLayerId).contains(active)) {
                revokeSwapGrants(player, targetLayerId);
            }
        }
    }
}
