package dev.overgrown.origins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public final class ForceSwapAction implements ActionType<EntityCtx, ForceSwapAction.Cfg> {

    public enum Mode implements StringRepresentable {
        LINEAR("linear"),
        RANDOM("random"),
        MAIN("main");

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

    public record Cfg(Mode mode, Optional<ResourceLocation> layer, Optional<ResourceLocation> origin,
                      boolean includeMain) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Mode.CODEC.optionalFieldOf("mode", Mode.LINEAR).forGetter(Cfg::mode),
            ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(Cfg::layer),
            ResourceLocation.CODEC.optionalFieldOf("origin").forGetter(Cfg::origin),
            Codec.BOOL.optionalFieldOf("include_main", true).forGetter(Cfg::includeMain)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.entity() instanceof ServerPlayer player)) return;
        ResourceLocation targetLayerId = resolveLayer(cfg);
        if (targetLayerId == null) return;
        if (cfg.origin.isPresent()) {
            SwapManager.applySwap(player, targetLayerId, cfg.origin.get());
            return;
        }
        switch (cfg.mode) {
            case LINEAR -> SwapManager.cycle(player, targetLayerId, false);
            case RANDOM -> SwapManager.random(player, targetLayerId, cfg.includeMain);
            case MAIN -> SwapManager.resetToMain(player, targetLayerId);
        }
    }

    static ResourceLocation resolveLayer(Cfg cfg) {
        return SwapManager.resolveTarget(cfg.layer.orElse(null));
    }
}
