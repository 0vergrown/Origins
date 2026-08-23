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
        return new ResourceLocation(targetLayerId.getNamespace(), "swap/" + targetLayerId.getPath());
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

    public static boolean revokeFromAnyPool(ServerPlayer player, ResourceLocation layerId, ResourceLocation originId) {
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer != null && layer.swappable()) return revokeFromPool(player, layerId, originId);
        for (OriginLayer swapLayer : swapLayersFor(layerId)) {
            if (revokeFromPool(player, swapLayer.id(), originId)) return true;
        }
        return false;
    }

    public static List<ResourceLocation> grantedPoolOf(Player player, ResourceLocation layerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return List.of();
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer != null && layer.swappable()) return List.copyOf(state.poolOf(layerId));
        List<ResourceLocation> out = new ArrayList<>();
        for (OriginLayer swapLayer : swapLayersFor(layerId)) {
            for (ResourceLocation id : state.poolOf(swapLayer.id())) {
                if (!out.contains(id)) out.add(id);
            }
        }
        return out;
    }

    public static @Nullable ResourceLocation grantTargetFor(ResourceLocation layerId) {
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer == null) return null;
        if (layer.swappable()) return layerId;
        List<OriginLayer> swapLayers = swapLayersFor(layerId);
        return swapLayers.isEmpty() ? null : swapLayers.get(0).id();
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
        if (OriginRegistry.get(resolvedIncoming) == null) return false;

        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return false;

        state.setActiveSwap(targetLayerId, incomingId);
        reconcile(state, container, targetLayerId);
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
        boolean wasSwapped = state.getActiveSwap(targetLayerId) != null;
        if (!wasSwapped && swapLayersFor(targetLayerId).isEmpty()) return;
        state.setActiveSwap(targetLayerId, null);
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        boolean changed = container != null && reconcile(state, container, targetLayerId);
        if (wasSwapped || changed) broadcast(player);
    }

    public static void revokeAllSwaps(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        for (ResourceLocation targetLayerId : List.copyOf(state.swapSnapshot().keySet())) {
            revokeSwapGrants(player, targetLayerId);
        }
    }

    private static boolean reconcile(PlayerOriginsImpl state, PowerContainer container,
                                     ResourceLocation targetLayerId) {
        ResourceLocation source = sourceFor(targetLayerId);
        ResourceLocation activeId = state.getActiveSwap(targetLayerId);
        Origin active = activeId == null ? null : OriginRegistry.get(activeId);
        boolean changed = false;
        if (activeId != null && active == null) {
            state.setActiveSwap(targetLayerId, null);
            changed = true;
        }

        Set<ResourceLocation> granted = active == null ? Set.of() : new HashSet<>(active.powers());
        for (ResourceLocation power : container.allPowers()) {
            if (granted.contains(power)) continue;
            if (container.sourcesOf(power).contains(source) && container.removePower(power, source)) changed = true;
        }
        for (ResourceLocation power : granted) {
            if (!container.sourcesOf(power).contains(source) && container.addPower(power, source)) changed = true;
        }

        Set<ResourceLocation> hidden = Set.of();
        if (active != null) {
            Origin main = OriginRegistry.get(state.getOrigin(targetLayerId));
            if (main != null) {
                Set<ResourceLocation> hide = new HashSet<>();
                for (ResourceLocation power : main.powers()) {
                    if (!granted.contains(power)) hide.add(power);
                }
                hidden = hide;
            }
        }
        for (ResourceLocation power : container.directlySuppressedPowers()) {
            if (hidden.contains(power)) continue;
            if (container.suppressionSourcesOf(power).contains(source)
                && container.unsuppressPower(power, source)) changed = true;
        }
        if (!hidden.isEmpty() && container.suppressAll(hidden, source)) changed = true;
        return changed;
    }

    public static void reconcileAll(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;

        Set<ResourceLocation> targets = new LinkedHashSet<>(state.swapSnapshot().keySet());
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (!layer.swappable() && !swapLayersFor(layer.id()).isEmpty()) targets.add(layer.id());
        }
        for (ResourceLocation targetLayerId : targets) reconcile(state, container, targetLayerId);
    }

    public static void revalidate(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        for (ResourceLocation targetLayerId : List.copyOf(state.swapSnapshot().keySet())) {
            ResourceLocation active = state.getActiveSwap(targetLayerId);
            if (active == null) continue;
            OriginLayer target = OriginLayers.get(targetLayerId);
            if (target == null || !target.enabled() || target.swappable()
                || !holdsSwapLayer(player, targetLayerId)
                || !pool(player, targetLayerId).contains(active)) {
                revokeSwapGrants(player, targetLayerId);
            }
        }
    }

    public static boolean holdsSwapLayer(Player player, ResourceLocation targetLayerId) {
        Map<ResourceLocation, ? extends Collection<ResourceLocation>> granted = granted(player);
        for (OriginLayer swapLayer : swapLayersFor(targetLayerId)) {
            if (!swapLayer.availableOrigins(player).isEmpty()) return true;
            Collection<ResourceLocation> explicit = granted.get(swapLayer.id());
            if (explicit != null && !explicit.isEmpty()) return true;
        }
        return false;
    }
}
