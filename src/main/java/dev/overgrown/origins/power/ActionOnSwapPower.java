package dev.overgrown.origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ActionOnSwapPower extends PowerType<ActionOnSwapPower.Config> {

    public record Config(
        Optional<EntityAction> entityAction,
        Optional<ResourceLocation> layer,
        Optional<ResourceLocation> fromOrigin,
        Optional<ResourceLocation> toOrigin
    ) {}

    @Override
    public MapCodec<Config> configCodec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            dev.overgrown.apoli.codec.LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Config::entityAction),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Config::layer),
            ResourceLocation.CODEC.optionalFieldOf("from_origin").forGetter(Config::fromOrigin),
            ResourceLocation.CODEC.optionalFieldOf("to_origin").forGetter(Config::toOrigin)
        ).apply(i, Config::new));
    }

    public static void fire(ServerPlayer player, ResourceLocation layerId,
                            @Nullable ResourceLocation from, ResourceLocation to) {
        EntityCtx ctx = new EntityCtx(player, player.level());
        PowerLookup.forEach(player, Origins.id("action_on_swap"), Config.class, cfg -> {
            if (cfg.layer().isPresent() && !cfg.layer().get().equals(layerId)) return;
            if (cfg.fromOrigin().isPresent() && !cfg.fromOrigin().get().equals(from)) return;
            if (cfg.toOrigin().isPresent() && !cfg.toOrigin().get().equals(to)) return;
            cfg.entityAction().ifPresent(action -> action.run(ctx));
        });
    }
}
