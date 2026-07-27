package dev.overgrown.origins.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record ConditionedOrigin(@Nullable EntityCondition condition, List<ResourceLocation> origins) {
    public ConditionedOrigin {
        origins = List.copyOf(origins);
    }

    public static final Codec<ConditionedOrigin> CODEC = Codec.either(
        ResourceLocation.CODEC,
        RecordCodecBuilder.<ConditionedOrigin>create(instance -> instance.group(
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(co -> Optional.ofNullable(co.condition())),
            ResourceLocation.CODEC.listOf().optionalFieldOf("origins", List.of()).forGetter(ConditionedOrigin::origins)
        ).apply(instance, (condition, origins) -> new ConditionedOrigin(condition.orElse(null), origins)))
    ).xmap(
        either -> either.map(id -> new ConditionedOrigin(null, List.of(id)), Function.identity()),
        co -> (co.condition() == null && co.origins().size() == 1)
            ? Either.left(co.origins().get(0))
            : Either.right(co)
    );

    public boolean test(Player player) {
        if (condition == null) return true;
        return condition.test(new EntityCtx(player, player.level()));
    }

    public void write(FriendlyByteBuf buf) {
        Optional<EntityCondition> opt = Optional.ofNullable(condition);
        buf.writeOptional(opt, (b, c) -> EntityCondition.CODEC.encodeStart(JsonOps.INSTANCE, c)
            .resultOrPartial(err -> {
                throw new RuntimeException("encode condition: " + err);
            })
            .ifPresent(json -> b.writeUtf(json.toString())));
        buf.writeCollection(origins, FriendlyByteBuf::writeResourceLocation);
    }

    public static ConditionedOrigin read(FriendlyByteBuf buf) {
        EntityCondition condition = buf.readOptional(b -> {
            String json = b.readUtf(32767);
            return EntityCondition.CODEC.parse(JsonOps.INSTANCE, net.minecraft.util.GsonHelper.parse(json))
                .resultOrPartial(err -> {})
                .orElse(null);
        }).orElse(null);
        List<ResourceLocation> ids = buf.readList(FriendlyByteBuf::readResourceLocation);
        return new ConditionedOrigin(condition, ids);
    }
}
