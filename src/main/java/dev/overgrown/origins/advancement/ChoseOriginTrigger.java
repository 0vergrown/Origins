package dev.overgrown.origins.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.codec.IdCodecs;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class ChoseOriginTrigger extends SimpleCriterionTrigger<ChoseOriginTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, ResourceLocation layer, ResourceLocation origin) {
        this.trigger(player, instance -> instance.matches(layer, origin));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<ResourceLocation> origin,
                                  Optional<ResourceLocation> layer) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
            IdCodecs.ID.optionalFieldOf("origin").forGetter(TriggerInstance::origin),
            IdCodecs.ID.optionalFieldOf("layer").forGetter(TriggerInstance::layer)
        ).apply(i, TriggerInstance::new));

        public boolean matches(ResourceLocation onLayer, ResourceLocation chosen) {
            if (layer.isPresent() && !layer.get().equals(onLayer)) return false;
            return origin.isEmpty() || origin.get().equals(chosen);
        }
    }
}
