package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.SwapManager;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class GrantOriginAction implements ActionType<EntityCtx, GrantOriginAction.Cfg> {

    public record Cfg(ResourceLocation origin, ResourceLocation layer, boolean toPool) {}

    @Override
    public MapCodec<Cfg> codec() {
        return dev.overgrown.apoli.alias.AliasingMapCodec.wrap(
            RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("origin").forGetter(Cfg::origin),
                ResourceLocation.CODEC.optionalFieldOf("layer", OriginStorage.DEFAULT_LAYER).forGetter(Cfg::layer),
                Codec.BOOL.optionalFieldOf("to_pool", false).forGetter(Cfg::toPool)
            ).apply(i, Cfg::new)),
            java.util.Map.of("swappable", "to_pool"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        if (OriginRegistry.get(cfg.origin) == null || cfg.origin.equals(OriginRegistry.EMPTY_ID)) return;
        OriginLayer layer = OriginLayers.get(cfg.layer);
        if (layer == null) return;
        if (layer.swappable()) {
            SwapManager.grantToPool(player, cfg.layer, cfg.origin);
            return;
        }
        if (cfg.toPool) {
            ResourceLocation poolLayer = SwapManager.grantTargetFor(cfg.layer);
            if (poolLayer != null) SwapManager.grantToPool(player, poolLayer, cfg.origin);
            return;
        }
        OriginManager.chooseOrigin(player, cfg.layer, cfg.origin, false);
    }
}
