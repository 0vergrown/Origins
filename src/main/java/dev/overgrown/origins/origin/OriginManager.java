package dev.overgrown.origins.origin;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ActionOnCallbackPower;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OriginManager {
    private OriginManager() {}

    private static ResourceLocation sourceFor(ResourceLocation layerId) {
        return new ResourceLocation(layerId.getNamespace(), "layer/" + layerId.getPath());
    }

    public static void chooseOrigin(ServerPlayer player, ResourceLocation layerId,
                                    ResourceLocation originId, boolean fromOrb) {
        OriginLayer layer = OriginLayers.get(layerId);
        Origin origin = OriginRegistry.get(originId);
        if (layer == null || origin == null) {
            Origins.LOGGER.warn("chooseOrigin: unknown layer={} or origin={}", layerId, originId);
            return;
        }
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        state.setOrigin(layerId, originId);
        applyOriginPowers(player, layer, origin);
        checkAutoChoosingLayers(player, false);
        revalidateGatedLayers(player);

        if (hasChosenAllLayers(player, state)) {
            state.setSelectingOrigin(false);
            ActionOnCallbackPower.fireChosen(player, fromOrb);
        }
    }

    public static void removeOrigin(ServerPlayer player, ResourceLocation layerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || !state.hasOrigin(layerId)) return;
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) container.removeAllFromSource(sourceFor(layerId));
        state.clearOrigin(layerId);
        revalidateGatedLayers(player);
    }

    public static boolean transferOrigin(ServerPlayer donor, ServerPlayer recipient,
                                         ResourceLocation fromLayer, ResourceLocation toLayer, boolean copy) {
        PlayerOriginsImpl donorState = PlayerOriginsAttachment.get(donor);
        if (donorState == null) return false;
        ResourceLocation originId = donorState.getOrigin(fromLayer);
        if (originId == null) return false;
        if (OriginLayers.get(toLayer) == null || OriginRegistry.get(originId) == null) return false;

        chooseOrigin(recipient, toLayer, originId, false);

        boolean donorChanged = !copy && !(donor == recipient && fromLayer.equals(toLayer));
        if (donorChanged) removeOrigin(donor, fromLayer);

        MinecraftServer server = recipient.getServer();
        if (server != null) {
            OriginsServerNetwork.broadcastPlayerOrigins(server, recipient);
            if (donorChanged && donor != recipient) OriginsServerNetwork.broadcastPlayerOrigins(server, donor);
        }
        return true;
    }

    public static void reapplyAll(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        for (var entry : state.snapshot().entrySet()) {
            OriginLayer layer = OriginLayers.get(entry.getKey());
            Origin origin = OriginRegistry.get(entry.getValue());
            if (layer == null || origin == null) continue;
            applyOriginPowers(player, layer, origin);
            if (container != null) {
                for (ResourceLocation powerId : origin.powers()) {
                    if (ApoliPowers.get(powerId) == null) container.removeAllFromSource(powerId);
                }
            }
        }
        revalidateGatedLayers(player);
    }

    public static boolean checkAutoChoosingLayers(ServerPlayer player, boolean includeDefaults) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        boolean chose = false;
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (state.hasOrigin(layer.id())) continue;
            ResourceLocation pick = autoPick(player, layer, includeDefaults);
            if (pick == null) continue;
            Origin origin = OriginRegistry.get(pick);
            if (origin == null) continue;
            state.setOrigin(layer.id(), pick);
            applyOriginPowers(player, layer, origin);
            chose = true;
        }
        if (chose) revalidateGatedLayers(player);
        return chose;
    }

    private static ResourceLocation autoPick(Player player, OriginLayer layer, boolean includeDefaults) {
        ResourceLocation only = null;
        int choosable = 0;
        for (ResourceLocation id : layer.availableOrigins(player)) {
            Origin origin = OriginRegistry.get(id);
            if (origin == null || !origin.choosable()) continue;
            if (++choosable > 1) break;
            only = id;
        }
        if (choosable == 1 && layer.autoChooseIfNoChoice()) return only;
        if (choosable > 0 || !includeDefaults || hasChoosableOrigins(player, layer)) return null;
        ResourceLocation defaultOrigin = layer.defaultOrigin();
        return defaultOrigin != null && OriginRegistry.get(defaultOrigin) != null ? defaultOrigin : null;
    }

    private static void revalidateGatedLayers(Player player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        for (ResourceLocation layerId : List.copyOf(state.snapshot().keySet())) {
            if (!state.hasOrigin(layerId)) continue;
            OriginLayer layer = OriginLayers.get(layerId);
            if (layer == null) continue;
            if (!layer.availableOrigins(player).isEmpty()) continue;
            if (container != null) container.removeAllFromSource(sourceFor(layerId));
            state.clearOrigin(layerId);
        }
    }

    private static void applyOriginPowers(Player player, OriginLayer layer, Origin origin) {
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container == null) return;
        ResourceLocation source = sourceFor(layer.id());
        Set<ResourceLocation> desired = new HashSet<>(origin.powers());
        Set<ResourceLocation> current = new HashSet<>();
        for (ResourceLocation power : container.allPowers()) {
            if (container.sourcesOf(power).contains(source)) current.add(power);
        }
        for (ResourceLocation power : current) {
            if (!desired.contains(power)) container.removePower(power, source);
        }
        for (ResourceLocation power : origin.powers()) {
            if (!current.contains(power)) container.addPower(power, source);
        }
    }

    public static boolean hasChosenAllLayers(Player player, PlayerOriginsImpl state) {
        for (OriginLayer layer : OriginLayers.enabledFor(player)) {
            if (!hasChoosableOrigins(player, layer)) continue;
            if (!state.hasOrigin(layer.id())) return false;
        }
        return true;
    }

    public static OriginLayer firstUnchosenLayer(Player player, PlayerOriginsImpl state) {
        for (OriginLayer layer : OriginLayers.enabledFor(player)) {
            if (!hasChoosableOrigins(player, layer)) continue;
            if (!state.hasOrigin(layer.id())) return layer;
        }
        return null;
    }

    public static boolean hasChoosableOrigins(Player player, OriginLayer layer) {
        boolean randomRollsUnchoosable = layer.allowRandom() && layer.randomAllowsUnchoosable();
        for (ResourceLocation id : layer.availableOrigins(player)) {
            Origin origin = OriginRegistry.get(id);
            if (origin == null) continue;
            if (origin.choosable() || randomRollsUnchoosable) return true;
        }
        return false;
    }
}
