package dev.overgrown.origins.condition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginPattern;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OriginCondition implements ConditionType<EntityCtx, OriginCondition.Cfg> {
    public record Cfg(List<OriginPattern> origins, Optional<ResourceLocation> layer,
                      dev.overgrown.origins.origin.OriginSelection selection) {
        public Cfg {
            origins = List.copyOf(origins);
        }

        public boolean matches(ResourceLocation id) {
            if (id == null) return false;
            for (int i = 0; i < origins.size(); i++) {
                if (origins.get(i).matches(id)) return true;
            }
            return false;
        }
    }

    private static final Codec<List<OriginPattern>> ORIGINS_CODEC = Codec.either(
        OriginPattern.CODEC, OriginPattern.CODEC.listOf()
    ).xmap(
        either -> either.map(List::of, list -> list),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
    );

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ORIGINS_CODEC.fieldOf("origin").forGetter(Cfg::origins),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer),
            dev.overgrown.origins.origin.OriginSelection.CODEC
                .optionalFieldOf("selection", dev.overgrown.origins.origin.OriginSelection.MAIN)
                .forGetter(Cfg::selection)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        dev.overgrown.origins.origin.OriginSelection mode = cfg.selection;
        boolean wantsMain = mode == dev.overgrown.origins.origin.OriginSelection.MAIN
            || mode == dev.overgrown.origins.origin.OriginSelection.ALL;
        boolean wantsActive = mode == dev.overgrown.origins.origin.OriginSelection.ACTIVE
            || mode == dev.overgrown.origins.origin.OriginSelection.ALL;
        boolean wantsPool = mode == dev.overgrown.origins.origin.OriginSelection.POOL
            || mode == dev.overgrown.origins.origin.OriginSelection.ALL;

        if (cfg.layer.isPresent()) {
            ResourceLocation layer = cfg.layer.get();
            if (wantsMain && cfg.matches(OriginView.chosen(player).get(layer))) return true;
            if (wantsActive && cfg.matches(OriginView.activeOn(player, layer))) return true;
            if (wantsPool) {
                for (ResourceLocation id : OriginView.pool(player)) {
                    if (cfg.matches(id)) return true;
                }
            }
            return false;
        }
        if (wantsMain) {
            for (ResourceLocation held : OriginView.chosen(player).values()) {
                if (cfg.matches(held)) return true;
            }
        }
        if (wantsActive) {
            for (ResourceLocation held : OriginView.swaps(player).values()) {
                if (cfg.matches(held)) return true;
            }
            for (ResourceLocation held : OriginView.chosen(player).values()) {
                if (cfg.matches(held)) return true;
            }
        }
        if (wantsPool) {
            for (ResourceLocation id : OriginView.pool(player)) {
                if (cfg.matches(id)) return true;
            }
        }
        return false;
    }
}
