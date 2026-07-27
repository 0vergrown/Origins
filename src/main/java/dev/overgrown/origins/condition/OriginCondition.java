package dev.overgrown.origins.condition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Optional;

public final class OriginCondition implements ConditionType<EntityCtx, OriginCondition.Cfg> {
    public record Cfg(ResourceLocation origin, Optional<ResourceLocation> layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.fieldOf("origin").forGetter(Cfg::origin),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        Map<ResourceLocation, ResourceLocation> origins = originsOf(player, ctx.level().isClientSide());
        return cfg.layer
            .map(layer -> cfg.origin.equals(origins.get(layer)))
            .orElseGet(() -> origins.containsValue(cfg.origin));
    }

    private static Map<ResourceLocation, ResourceLocation> originsOf(Player player, boolean clientSide) {
        if (clientSide) {

            return OriginsClientState.get(player.getUUID());
        }
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? Map.of() : state.snapshot();
    }
}
