package dev.overgrown.origins.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class OpenSwapMenuAction implements ActionType<EntityCtx, OpenSwapMenuAction.Cfg> {

    public record Cfg(Optional<ResourceLocation> layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        ResourceLocation target = SwapManager.resolveTarget(cfg.layer.orElse(null));
        if (target == null) return;
        OriginsServerNetwork.openSwapScreen(player, target);
    }
}
