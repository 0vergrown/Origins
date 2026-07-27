package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.world.entity.player.Player;

public final class StoreValueAction implements ActionType<EntityCtx, StoreValueAction.Cfg> {
    public record Cfg(String key, String value, boolean clear) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Cfg::key),
            Codec.STRING.optionalFieldOf("value", "").forGetter(Cfg::value),
            Codec.BOOL.optionalFieldOf("clear", false).forGetter(Cfg::clear)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player) || ctx.level().isClientSide()) return;
        if (cfg.clear) {
            OriginStorage.clear(player, cfg.key);
            return;
        }
        String resolved = OriginStorage.resolve(player, cfg.value);
        if (resolved == null) return;
        OriginStorage.storeValue(player, cfg.key, resolved);
    }
}
