package dev.overgrown.origins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.storage.StoredData;
import dev.overgrown.origins.storage.StoredDataAttachment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StoredOriginCondition implements ConditionType<EntityCtx, StoredOriginCondition.Cfg> {
    public record Cfg(Optional<String> key, Optional<ResourceLocation> origin, Optional<ResourceLocation> layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("key").forGetter(Cfg::key),
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof Player player)) return false;
        StoredData data = StoredDataAttachment.get(player);
        if (data == null) return false;
        if (cfg.key.isPresent()) {
            return matches(data.getOrigin(cfg.key.get()), cfg);
        }
        for (StoredData.StoredOrigin stored : data.originsView().values()) {
            if (matches(stored, cfg)) return true;
        }
        return false;
    }

    private static boolean matches(StoredData.StoredOrigin stored, Cfg cfg) {
        if (stored == null) return false;
        if (cfg.origin.isPresent() && !cfg.origin.get().equals(stored.origin())) return false;
        return cfg.layer.isEmpty() || cfg.layer.get().equals(stored.layer());
    }
}
