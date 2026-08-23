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
    public record Cfg(ResourceLocation fromLayer, ResourceLocation toLayer,
                      java.util.Optional<ResourceLocation> origin,
                      java.util.Optional<dev.overgrown.origins.origin.OriginSelection> selection) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("from_layer", Origins.id("origin")).forGetter(Cfg::fromLayer),
            ResourceLocation.CODEC.optionalFieldOf("to_layer", Origins.id("copy")).forGetter(Cfg::toLayer),
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
            dev.overgrown.origins.origin.OriginSelection.CODEC.optionalFieldOf("selection").forGetter(Cfg::selection)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof ServerPlayer actor)) return;
        if (!(ctx.target() instanceof ServerPlayer target)) return;

        OriginManager.transferOrigin(target, actor, cfg.fromLayer, cfg.toLayer, true,
            cfg.selection.orElse(null), cfg.origin.orElse(null), false);
    }
}
