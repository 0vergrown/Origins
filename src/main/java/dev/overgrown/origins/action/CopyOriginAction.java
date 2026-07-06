package dev.overgrown.origins.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;


public final class CopyOriginAction implements ActionType<BiEntityCtx, CopyOriginAction.Cfg> {
    public record Cfg(ResourceLocation fromLayer, ResourceLocation toLayer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("from_layer", Origins.id("origin")).forGetter(Cfg::fromLayer),
            ResourceLocation.CODEC.optionalFieldOf("to_layer", Origins.id("copy")).forGetter(Cfg::toLayer)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof ServerPlayer actor)) return;
        if (!(ctx.target() instanceof ServerPlayer target)) return;
        
        
        OriginManager.transferOrigin(target, actor, cfg.fromLayer, cfg.toLayer, true);
    }
}
