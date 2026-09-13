package dev.overgrown.origins.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TextComponent;
import dev.overgrown.origins.Origins;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record OriginUpgrade(EntityCondition condition, ResourceLocation origin, Optional<Component> announcement) {

    public static final Codec<OriginUpgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EntityCondition.CODEC.fieldOf("condition").forGetter(OriginUpgrade::condition),
        dev.overgrown.apoli.codec.IdCodecs.ID.fieldOf("origin").forGetter(OriginUpgrade::origin),
        TextComponent.CODEC.optionalFieldOf("announcement").forGetter(OriginUpgrade::announcement)
    ).apply(instance, OriginUpgrade::new));

    public boolean test(Player player) {
        return condition.test(new EntityCtx(player, player.level()));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(EntityCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition)
            .resultOrPartial(err -> Origins.LOGGER.error(
                "Upgrade to {} could not be sent to clients — its condition failed to encode: {}", origin, err))
            .map(Object::toString)
            .orElse(""));
        buf.writeResourceLocation(origin);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, announcement);
    }

    public static @Nullable OriginUpgrade read(RegistryFriendlyByteBuf buf) {
        String json = buf.readUtf(32767);
        EntityCondition condition = json.isEmpty() ? null : EntityCondition.CODEC
            .parse(JsonOps.INSTANCE, net.minecraft.util.GsonHelper.parse(json))
            .resultOrPartial(err -> Origins.LOGGER.error("Dropped a synced origin upgrade — condition: {}", err))
            .orElse(null);
        ResourceLocation origin = buf.readResourceLocation();
        Optional<Component> announcement = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
        return condition == null ? null : new OriginUpgrade(condition, origin, announcement);
    }
}
