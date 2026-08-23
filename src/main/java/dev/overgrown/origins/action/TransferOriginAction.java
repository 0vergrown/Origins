package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class TransferOriginAction implements ActionType<BiEntityCtx, TransferOriginAction.Cfg> {

    public enum Mode implements StringRepresentable {
        STEAL("steal"),
        GIVE("give");

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;
        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(
        Mode mode,
        boolean copy,
        ResourceLocation fromLayer,
        ResourceLocation toLayer,
        Optional<EntityAction> actorAction,
        Optional<EntityAction> targetAction,
        Optional<ResourceLocation> origin,
        Optional<dev.overgrown.origins.origin.OriginSelection> selection,
        boolean random
    ) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.STEAL).forGetter(Cfg::mode),
            Codec.BOOL.optionalFieldOf("copy", false).forGetter(Cfg::copy),
            ResourceLocation.CODEC.optionalFieldOf("from_layer", Origins.id("origin")).forGetter(Cfg::fromLayer),
            ResourceLocation.CODEC.optionalFieldOf("to_layer", Origins.id("origin")).forGetter(Cfg::toLayer),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("actor_action", EntityAction.CODEC).forGetter(Cfg::actorAction),
            dev.overgrown.apoli.codec.LoggedOptionalField.of("target_action", EntityAction.CODEC).forGetter(Cfg::targetAction),
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
            dev.overgrown.origins.origin.OriginSelection.CODEC.optionalFieldOf("selection").forGetter(Cfg::selection),
            Codec.BOOL.optionalFieldOf("random", false).forGetter(Cfg::random)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (ctx.actor() instanceof ServerPlayer actor && ctx.target() instanceof ServerPlayer target) {
            ServerPlayer donor = cfg.mode == Mode.STEAL ? target : actor;
            ServerPlayer recipient = cfg.mode == Mode.STEAL ? actor : target;
            OriginManager.transferOrigin(donor, recipient, cfg.fromLayer, cfg.toLayer, cfg.copy,
                cfg.selection.orElse(null), cfg.origin.orElse(null), cfg.random);
        }
        cfg.actorAction.ifPresent(a -> a.run(ctx.asActor()));
        cfg.targetAction.ifPresent(a -> a.run(ctx.asTarget()));
    }
}
