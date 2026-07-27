package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ApplyStoredOriginAction implements ActionType<EntityCtx, ApplyStoredOriginAction.Cfg> {
    public record Cfg(String key, Optional<ResourceLocation> layer, boolean clear) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Cfg::key),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer),
            Codec.BOOL.optionalFieldOf("clear", false).forGetter(Cfg::clear)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        OriginStorage.applyStoredOrigin(player, cfg.key, cfg.layer.orElse(null), cfg.clear);
    }
}
