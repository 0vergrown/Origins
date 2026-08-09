package dev.overgrown.origins.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SwappedCondition implements ConditionType<EntityCtx, SwappedCondition.Cfg> {

    public record Cfg(Optional<ResourceLocation> layer, Optional<ResourceLocation> origin) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer),
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        ResourceLocation target = SwapManager.resolveTarget(cfg.layer.orElse(null));
        if (target == null) return false;
        ResourceLocation active = SwapManager.activeSwap(player, target);
        if (active == null) return false;
        return cfg.origin.isEmpty() || cfg.origin.get().equals(active);
    }
}
