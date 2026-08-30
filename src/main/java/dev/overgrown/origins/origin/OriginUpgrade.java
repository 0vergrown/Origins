package dev.overgrown.origins.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record OriginUpgrade(EntityCondition condition, ResourceLocation origin, Optional<Component> announcement) {

    public static final Codec<OriginUpgrade> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        EntityCondition.CODEC.fieldOf("condition").forGetter(OriginUpgrade::condition),
        ResourceLocation.CODEC.fieldOf("origin").forGetter(OriginUpgrade::origin),
        TextComponent.CODEC.optionalFieldOf("announcement").forGetter(OriginUpgrade::announcement)
    ).apply(instance, OriginUpgrade::new));

    public boolean test(Player player) {
        return condition.test(new EntityCtx(player, player.level()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(EntityCondition.CODEC.encodeStart(JsonOps.INSTANCE, condition)
            .resultOrPartial(err -> {
                throw new RuntimeException("encode upgrade condition: " + err);
            })
            .orElseThrow()
            .toString());
        buf.writeResourceLocation(origin);
        buf.writeOptional(announcement, FriendlyByteBuf::writeComponent);
    }

    public static @Nullable OriginUpgrade read(FriendlyByteBuf buf) {
        String json = buf.readUtf(32767);
        EntityCondition condition = EntityCondition.CODEC
            .parse(JsonOps.INSTANCE, net.minecraft.util.GsonHelper.parse(json))
            .resultOrPartial(err -> {})
            .orElse(null);
        ResourceLocation origin = buf.readResourceLocation();
        Optional<Component> announcement = buf.readOptional(FriendlyByteBuf::readComponent);
        return condition == null ? null : new OriginUpgrade(condition, origin, announcement);
    }
}
