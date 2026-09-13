package dev.overgrown.origins.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public final class ImpactCondition implements ConditionType<EntityCtx, ImpactCondition.Cfg> {

    public enum Aggregate implements StringRepresentable {
        MAX("max"),
        MIN("min"),
        SUM("sum"),
        AVERAGE("average");

        public static final Codec<Aggregate> CODEC = StringRepresentable.fromEnum(Aggregate::values);

        private final String name;

        Aggregate(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Cfg(Optional<ResourceLocation> layer, Aggregate aggregate,
                      Comparison comparison, int compareTo) {}

    public static MapCodec<Optional<ResourceLocation>> layerField() {
        return ResourceLocation.CODEC.optionalFieldOf("layer");
    }

    public static MapCodec<Aggregate> aggregateField() {
        return Aggregate.CODEC.optionalFieldOf("aggregate", Aggregate.MAX);
    }

    public static double impactOf(@Nullable Entity entity, Optional<ResourceLocation> layer, Aggregate aggregate) {
        if (!(entity instanceof Player player)) return -1.0;
        if (layer.isPresent()) return OriginView.impactOn(player, layer.get());

        Map<ResourceLocation, ResourceLocation> chosen = OriginView.chosen(player);
        if (chosen.isEmpty()) return 0.0;

        int count = 0;
        int sum = 0;
        int max = 0;
        int min = Integer.MAX_VALUE;
        for (ResourceLocation layerId : chosen.keySet()) {
            ResourceLocation active = OriginView.activeOn(player, layerId);
            if (active == null) continue;
            Origin origin = OriginRegistry.get(active);
            if (origin == null) continue;
            int level = origin.impact().level();
            count++;
            sum += level;
            if (level > max) max = level;
            if (level < min) min = level;
        }
        if (count == 0) return 0.0;
        return switch (aggregate) {
            case MAX -> max;
            case MIN -> min;
            case SUM -> sum;
            case AVERAGE -> sum / (double) count;
        };
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            layerField().forGetter(Cfg::layer),
            aggregateField().forGetter(Cfg::aggregate),
            Comparison.CODEC.optionalFieldOf("comparison", Comparison.GREATER_EQUAL).forGetter(Cfg::comparison),
            dev.overgrown.origins.origin.Impact.LEVEL_CODEC.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        double impact = impactOf(ctx.raw(), cfg.layer, cfg.aggregate);
        if (impact < 0.0) return false;
        return cfg.comparison.compare(impact, cfg.compareTo);
    }
}
