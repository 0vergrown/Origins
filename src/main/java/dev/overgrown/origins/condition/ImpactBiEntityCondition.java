package dev.overgrown.origins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.data.Comparison;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class ImpactBiEntityCondition implements ConditionType<BiEntityCtx, ImpactBiEntityCondition.Cfg> {

    public enum Subject implements StringRepresentable {
        ACTOR("actor"),
        TARGET("target");

        public static final Codec<Subject> CODEC = StringRepresentable.fromEnum(Subject::values);

        private final String name;

        Subject(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(Subject subject, Optional<ResourceLocation> layer, ImpactCondition.Aggregate aggregate,
                      Comparison comparison, Optional<Integer> compareTo, double offset) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            Subject.CODEC.optionalFieldOf("subject", Subject.ACTOR).forGetter(Cfg::subject),
            ImpactCondition.layerField().forGetter(Cfg::layer),
            ImpactCondition.aggregateField().forGetter(Cfg::aggregate),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            dev.overgrown.origins.origin.Impact.LEVEL_CODEC.optionalFieldOf("compare_to").forGetter(Cfg::compareTo),
            Codec.DOUBLE.optionalFieldOf("offset", 0.0).forGetter(Cfg::offset)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, BiEntityCtx ctx) {
        boolean actorIsSubject = cfg.subject == Subject.ACTOR;
        Entity subject = actorIsSubject ? ctx.rawActor() : ctx.rawTarget();
        double left = ImpactCondition.impactOf(subject, cfg.layer, cfg.aggregate);
        if (left < 0.0) return false;

        double right;
        if (cfg.compareTo.isPresent()) {
            right = cfg.compareTo.get();
        } else {
            Entity other = actorIsSubject ? ctx.rawTarget() : ctx.rawActor();
            right = ImpactCondition.impactOf(other, cfg.layer, cfg.aggregate);
            if (right < 0.0) return false;
        }
        return cfg.comparison.compare(left, right + cfg.offset);
    }
}
