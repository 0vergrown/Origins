package dev.overgrown.origins.origin;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.builtin.meta.AllOfMeta;
import dev.overgrown.apoli.condition.builtin.meta.AnyOfMeta;
import dev.overgrown.apoli.power.builtin.ActionOnCallbackPower;
import dev.overgrown.apoli.power.builtin.ModifyPlayerSpawnHandler;
import dev.overgrown.apoli.skill.SkillTrees;
import dev.overgrown.origins.condition.OriginCondition;
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

    private static final int MAX_RECONCILE_PASSES = 8;
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
        if (!originId.equals(state.getOrigin(layerId))) SwapManager.revokeSwapGrants(player, layerId);
        state.setOrigin(layerId, originId);
        state.setPinned(layerId, true);
        applyOriginPowers(player, layer, origin);
        reconcileLayers(player);

        if (hasChosenAllLayers(player, state)) {
            state.setSelectingOrigin(false);
            ModifyPlayerSpawnHandler.teleportToModifiedSpawn(player);
            ActionOnCallbackPower.fireChosen(player, fromOrb);
        }
    }

    public static void removeOrigin(ServerPlayer player, ResourceLocation layerId) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return;
        OriginLayer target = OriginLayers.get(layerId);
        if (target != null && target.swappable()) {
            SwapManager.clearPool(player, layerId);
            return;
        }
        if (!state.hasOrigin(layerId)) return;
        SwapManager.revokeSwapGrants(player, layerId);
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        if (container != null) container.removeAllFromSource(sourceFor(layerId));
        state.clearOrigin(layerId);
        reconcileLayers(player);
    }

    public static boolean transferOrigin(ServerPlayer donor, ServerPlayer recipient,
                                         ResourceLocation fromLayer, ResourceLocation toLayer, boolean copy) {
        PlayerOriginsImpl donorState = PlayerOriginsAttachment.get(donor);
        if (donorState == null) return false;
        ResourceLocation originId = donorState.getOrigin(fromLayer);
        if (originId == null) return false;
        OriginLayer destination = OriginLayers.get(toLayer);
        if (destination == null || OriginRegistry.get(originId) == null) return false;

        if (destination.swappable()) {
            if (!SwapManager.grantToPool(recipient, toLayer, originId)) return false;
        } else {
            chooseOrigin(recipient, toLayer, originId, false);
        }

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
        SwapManager.reapplySuppression(player);
        reconcileLayers(player);
    }

    public static boolean checkAutoChoosingLayers(ServerPlayer player, boolean includeDefaults) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        boolean chose = false;
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.swappable()) continue;
            if (state.hasOrigin(layer.id())) continue;
            if (!dependenciesSettled(player, state, layer)) continue;
            ResourceLocation pick = autoPick(player, layer, includeDefaults);
            if (pick == null) continue;
            Origin origin = OriginRegistry.get(pick);
            if (origin == null) continue;
            state.setOrigin(layer.id(), pick);
            applyOriginPowers(player, layer, origin);
            chose = true;
        }
        return chose;
    }

    private static boolean dependenciesSettled(Player player, PlayerOriginsImpl state, OriginLayer layer) {
        Set<ResourceLocation> deps = new HashSet<>();
        boolean[] anyLayer = {false};
        for (ConditionedOrigin co : layer.conditionedOrigins()) {
            if (co.condition() != null) collectLayerDeps(co.condition(), deps, anyLayer);
        }
        if (anyLayer[0]) {
            for (OriginLayer other : OriginLayers.enabledOrdered()) {
                if (other.id().equals(layer.id())) continue;
                if (!settled(player, state, other)) return false;
            }
            return true;
        }
        for (ResourceLocation depId : deps) {
            if (depId.equals(layer.id())) continue;
            OriginLayer dep = OriginLayers.get(depId);
            if (dep == null || !dep.enabled()) continue;
            if (!settled(player, state, dep)) return false;
        }
        return true;
    }

    private static boolean settled(Player player, PlayerOriginsImpl state, OriginLayer layer) {
        return state.hasOrigin(layer.id()) || !hasChoosableOrigins(player, layer);
    }

    private static void collectLayerDeps(EntityCondition condition, Set<ResourceLocation> out, boolean[] anyLayer) {
        Object cfg = condition.config();
        if (cfg instanceof OriginCondition.Cfg originCfg) {
            originCfg.layer().ifPresentOrElse(out::add, () -> anyLayer[0] = true);
            return;
        }
        if (cfg instanceof AllOfMeta.Cfg<?> allOf) {
            for (Object nested : allOf.conditions()) {
                if (nested instanceof EntityCondition ec) collectLayerDeps(ec, out, anyLayer);
            }
            return;
        }
        if (cfg instanceof AnyOfMeta.Cfg<?> anyOf) {
            for (Object nested : anyOf.conditions()) {
                if (nested instanceof EntityCondition ec) collectLayerDeps(ec, out, anyLayer);
            }
        }
    }

    private static ResourceLocation autoPick(Player player, OriginLayer layer, boolean includeDefaults) {
        ResourceLocation onlyChoosable = null;
        ResourceLocation onlyAvailable = null;
        int choosable = 0;
        int available = 0;
        for (ResourceLocation id : layer.availableOrigins(player)) {
            Origin origin = OriginRegistry.get(id);
            if (origin == null) continue;
            if (++available == 1) onlyAvailable = id;
            if (origin.choosable() && ++choosable == 1) onlyChoosable = id;
        }
        if (layer.autoChooseIfNoChoice()) {
            if (choosable == 1) return onlyChoosable;
            if (choosable == 0 && available == 1) return onlyAvailable;
        }
        if (choosable > 0 || !includeDefaults || hasChoosableOrigins(player, layer)) return null;
        ResourceLocation defaultOrigin = layer.defaultOrigin();
        return defaultOrigin != null && OriginRegistry.get(defaultOrigin) != null ? defaultOrigin : null;
    }

    private static boolean redriveDerivedLayers(Player player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return false;
        boolean changed = false;
        for (ResourceLocation layerId : List.copyOf(state.snapshot().keySet())) {
            if (!state.hasOrigin(layerId)) continue;
            OriginLayer layer = OriginLayers.get(layerId);
            if (layer == null || !layer.revalidate()) continue;
            ResourceLocation held = state.getOrigin(layerId);
            if (held == null) continue;
            if (state.isPinned(layerId)) {
                if (layer.allOrigins().contains(held)) continue;
                state.setPinned(layerId, false);
            }
            List<ResourceLocation> available = layer.availableOrigins(player);
            if (available.contains(held)) continue;
            ResourceLocation replacement = derivedReplacement(layer, available);
            if (replacement == null || replacement.equals(held)) {
                Origins.LOGGER.debug("[Origins] Layer {} offers {} no replacement for {}; keeping it.",
                    layerId, player.getName().getString(), held);
                continue;
            }
            Origin origin = OriginRegistry.get(replacement);
            if (origin == null) continue;
            if (player instanceof ServerPlayer serverPlayer) SwapManager.revokeSwapGrants(serverPlayer, layerId);
            state.setOrigin(layerId, replacement);
            applyOriginPowers(player, layer, origin);
            changed = true;
        }
        return changed;
    }

    private static ResourceLocation derivedReplacement(OriginLayer layer, List<ResourceLocation> available) {
        ResourceLocation only = null;
        int count = 0;
        for (ResourceLocation id : available) {
            if (OriginRegistry.get(id) == null) continue;
            if (++count > 1) break;
            only = id;
        }
        if (count == 1) return only;
        ResourceLocation fallback = layer.defaultOrigin();
        return fallback != null && OriginRegistry.get(fallback) != null ? fallback : null;
    }

    public static void reconcileLayers(ServerPlayer player) {
        for (int pass = 0; pass < MAX_RECONCILE_PASSES; pass++) {
            boolean redriven = redriveDerivedLayers(player);
            boolean chose = checkAutoChoosingLayers(player, false);
            if (!redriven && !chose) {
                SwapManager.revalidate(player);
                SkillTrees.refresh(player);
                return;
            }
        }
        SkillTrees.refresh(player);
        Origins.LOGGER.warn("[Origins] Layer conditions for {} did not settle after {} passes; "
            + "check for origin layers whose conditions depend on each other in a cycle.",
            player.getName().getString(), MAX_RECONCILE_PASSES);
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
        if (layer.swappable()) return false;
        boolean randomRollsUnchoosable = layer.allowRandom() && layer.randomAllowsUnchoosable();
        for (ResourceLocation id : layer.availableOrigins(player)) {
            Origin origin = OriginRegistry.get(id);
            if (origin == null) continue;
            if (origin.choosable() || randomRollsUnchoosable) return true;
        }
        return false;
    }
}
