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

public record OriginPowerEntry(@Nullable EntityCondition condition, List<ResourceLocation> powers) {
    public OriginPowerEntry {
        powers = List.copyOf(powers);
    }

    public static final Codec<OriginPowerEntry> CODEC = Codec.either(
        ResourceLocation.CODEC,
        RecordCodecBuilder.<OriginPowerEntry>create(instance -> instance.group(
            EntityCondition.CODEC.optionalFieldOf("condition").forGetter(e -> Optional.ofNullable(e.condition())),
            ResourceLocation.CODEC.listOf().optionalFieldOf("powers", List.of()).forGetter(OriginPowerEntry::powers)
        ).apply(instance, (condition, powers) -> new OriginPowerEntry(condition.orElse(null), powers)))
    ).xmap(
        either -> either.map(id -> new OriginPowerEntry(null, List.of(id)), Function.identity()),
        e -> (e.condition() == null && e.powers().size() == 1)
            ? Either.left(e.powers().get(0))
            : Either.right(e)
    );

    public boolean visible(Player player) {
        if (condition == null) return true;
        return condition.test(new EntityCtx(player, player.level()));
    }

    public void write(FriendlyByteBuf buf) {
        Optional<EntityCondition> opt = Optional.ofNullable(condition);
        buf.writeOptional(opt, (b, c) -> EntityCondition.CODEC.encodeStart(JsonOps.INSTANCE, c)
            .resultOrPartial(err -> {
                throw new RuntimeException("encode power condition: " + err);
            })
            .ifPresent(json -> b.writeUtf(json.toString())));
        buf.writeCollection(powers, FriendlyByteBuf::writeResourceLocation);
    }

    public static OriginPowerEntry read(FriendlyByteBuf buf) {
        EntityCondition condition = buf.readOptional(b -> {
            String json = b.readUtf(32767);
            return EntityCondition.CODEC.parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
                .resultOrPartial(err -> {})
                .orElse(null);
        }).orElse(null);
        List<ResourceLocation> ids = buf.readList(FriendlyByteBuf::readResourceLocation);
        return new OriginPowerEntry(condition, ids);
    }
}
