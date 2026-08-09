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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OriginCondition implements ConditionType<EntityCtx, OriginCondition.Cfg> {
    public record Cfg(List<OriginPattern> origins, Optional<ResourceLocation> layer) {
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
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        Map<ResourceLocation, ResourceLocation> origins = originsOf(player, ctx.level().isClientSide());
        if (cfg.layer.isPresent()) return cfg.matches(origins.get(cfg.layer.get()));
        for (ResourceLocation held : origins.values()) {
            if (cfg.matches(held)) return true;
        }
        return false;
    }

    private static Map<ResourceLocation, ResourceLocation> originsOf(Player player, boolean clientSide) {
        if (clientSide) {
            return OriginsClientState.get(player.getUUID());
        }
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? Map.of() : state.snapshot();
    }
}
