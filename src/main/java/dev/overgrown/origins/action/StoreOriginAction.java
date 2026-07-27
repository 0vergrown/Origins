package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StoreOriginAction implements ActionType<EntityCtx, StoreOriginAction.Cfg> {
    public record Cfg(String key, ResourceLocation layer, Optional<ResourceLocation> origin, boolean clear) {}

    public static final MapCodec<Cfg> CONFIG_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("key").forGetter(Cfg::key),
        ResourceLocation.CODEC.optionalFieldOf("layer", OriginStorage.DEFAULT_LAYER).forGetter(Cfg::layer),
        ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
        Codec.BOOL.optionalFieldOf("clear", false).forGetter(Cfg::clear)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CONFIG_CODEC;
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player) || ctx.level().isClientSide()) return;
        apply(cfg, player, player);
    }

    public static void apply(Cfg cfg, Player holder, Player source) {
        if (cfg.clear()) {
            OriginStorage.clear(holder, cfg.key());
            return;
        }
        if (cfg.origin().isPresent()) {
            OriginStorage.storeOrigin(holder, cfg.key(), cfg.layer(), cfg.origin().get());
            return;
        }
        OriginStorage.storeOrigin(holder, cfg.key(), source, cfg.layer());
    }
}
