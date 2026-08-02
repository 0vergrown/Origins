package dev.overgrown.origins.origin;

import dev.overgrown.apoli.command.ApoliSelectorOptions;
import dev.overgrown.apoli.power.PowerSources;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class OriginPowerSources {

    private OriginPowerSources() {}

    public static void register() {
        PowerSources.register(new Provider());
        ApoliSelectorOptions.register("origin", new OriginOption());
        ApoliSelectorOptions.register("origin_layer", new LayerOption());
    }

    private static final class Provider implements PowerSources.Provider {
        @Override
        public @Nullable Collection<ResourceLocation> powersOf(ResourceLocation sourceId) {
            Origin origin = OriginRegistry.get(sourceId);
            if (origin != null) return origin.powers();

            OriginLayer layer = OriginLayers.get(sourceId);
            if (layer == null) return null;
            Set<ResourceLocation> out = new LinkedHashSet<>();
            for (ResourceLocation id : layer.allOrigins()) {
                Origin member = OriginRegistry.get(id);
                if (member != null) out.addAll(member.powers());
            }
            return out;
        }

        @Override
        public void collectSources(Collection<ResourceLocation> out) {
            for (Origin origin : OriginRegistry.all()) out.add(origin.id());
            for (OriginLayer layer : OriginLayers.all()) out.add(layer.id());
        }
    }

    private static final class OriginOption implements ApoliSelectorOptions.Handler {
        @Override
        public Predicate<Entity> predicate(ResourceLocation value) {
            return entity -> {
                if (!(entity instanceof Player player)) return false;
                PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
                if (state == null) return false;
                return state.snapshot().containsValue(value);
            };
        }

        @Override
        public Collection<ResourceLocation> suggestions() {
            List<ResourceLocation> ids = new ArrayList<>(OriginRegistry.size());
            for (Origin origin : OriginRegistry.all()) ids.add(origin.id());
            return ids;
        }
    }

    private static final class LayerOption implements ApoliSelectorOptions.Handler {
        @Override
        public Predicate<Entity> predicate(ResourceLocation value) {
            return entity -> {
                if (!(entity instanceof Player player)) return false;
                PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
                return state != null && state.hasOrigin(value);
            };
        }

        @Override
        public Collection<ResourceLocation> suggestions() {
            List<ResourceLocation> ids = new ArrayList<>(OriginLayers.size());
            for (OriginLayer layer : OriginLayers.all()) ids.add(layer.id());
            return ids;
        }
    }
}
