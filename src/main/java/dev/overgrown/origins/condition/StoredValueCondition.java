package dev.overgrown.origins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.storage.OriginStorage;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StoredValueCondition implements ConditionType<EntityCtx, StoredValueCondition.Cfg> {
    public record Cfg(String key, Optional<String> value) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Cfg::key),
            Codec.STRING.optionalFieldOf("value").forGetter(Cfg::value)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        String stored = OriginStorage.value(player, cfg.key);
        if (stored == null) return false;
        return cfg.value.isEmpty() || cfg.value.get().equals(stored);
    }
}
