package dev.overgrown.origins.condition;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.OriginSelection;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public final class OriginTagCondition implements ConditionType<EntityCtx, OriginTagCondition.Cfg> {

    public record Cfg(List<String> tags, Optional<ResourceLocation> layer, OriginSelection selection) {
        public Cfg {
            tags = List.copyOf(tags);
        }
    }

    private static final Codec<List<String>> TAGS_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
        either -> either.map(List::of, List::copyOf),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            TAGS_CODEC.fieldOf("tag").forGetter(Cfg::tags),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer),
            OriginSelection.CODEC.optionalFieldOf("selection", OriginSelection.MAIN).forGetter(Cfg::selection)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        OriginSelection mode = cfg.selection;
        boolean wantsMain = mode == OriginSelection.MAIN || mode == OriginSelection.ALL;
        boolean wantsActive = mode == OriginSelection.ACTIVE || mode == OriginSelection.ALL;
        boolean wantsPool = mode == OriginSelection.POOL || mode == OriginSelection.ALL;

        if (cfg.layer.isPresent()) {
            ResourceLocation layer = cfg.layer.get();
            if (wantsMain && matches(cfg, OriginView.chosen(player).get(layer))) return true;
            if (wantsActive && matches(cfg, OriginView.activeOn(player, layer))) return true;
            return wantsPool && matchesAny(cfg, OriginView.pool(player));
        }
        if (wantsMain && matchesAny(cfg, OriginView.chosen(player).values())) return true;
        if (wantsActive && (matchesAny(cfg, OriginView.swaps(player).values())
            || matchesAny(cfg, OriginView.chosen(player).values()))) return true;
        return wantsPool && matchesAny(cfg, OriginView.pool(player));
    }

    private static boolean matchesAny(Cfg cfg, Iterable<ResourceLocation> ids) {
        for (ResourceLocation id : ids) {
            if (matches(cfg, id)) return true;
        }
        return false;
    }

    private static boolean matches(Cfg cfg, ResourceLocation id) {
        if (id == null) return false;
        Origin origin = OriginRegistry.get(id);
        if (origin == null || origin.tags().isEmpty()) return false;
        for (int i = 0; i < cfg.tags.size(); i++) {
            if (origin.hasTag(cfg.tags.get(i))) return true;
        }
        return false;
    }
}
