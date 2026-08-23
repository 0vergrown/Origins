package dev.overgrown.origins.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.SwapManager;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class RevokeOriginAction implements ActionType<EntityCtx, RevokeOriginAction.Cfg> {

    public record Cfg(Optional<ResourceLocation> origin, ResourceLocation layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
            ResourceLocation.CODEC.optionalFieldOf("layer", OriginStorage.DEFAULT_LAYER).forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        OriginLayer layer = OriginLayers.get(cfg.layer);
        if (layer == null) return;
        if (cfg.origin.isEmpty()) {
            OriginManager.removeOrigin(player, cfg.layer);
            for (ResourceLocation granted : List.copyOf(SwapManager.grantedPoolOf(player, cfg.layer))) {
                SwapManager.revokeFromAnyPool(player, cfg.layer, granted);
            }
            return;
        }
        ResourceLocation originId = cfg.origin.get();
        if (SwapManager.revokeFromAnyPool(player, cfg.layer, originId)) return;
        if (!layer.swappable() && originId.equals(
            dev.overgrown.origins.component.PlayerOriginsAttachment.getOrCreate(player).getOrigin(cfg.layer))) {
            OriginManager.removeOrigin(player, cfg.layer);
        }
    }
}
