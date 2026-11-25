package dev.overgrown.origins.origin;

import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record OriginGui(Optional<ResourceLocation> background, Optional<ResourceLocation> border,
                        Optional<ResourceLocation> namePlate, Optional<ResourceLocation> scrollBar,
                        Optional<ResourceLocation> impact, Optional<Component> impactName) {

    private static final String IMAGE = ".png";

    private static final MapCodec<OriginGui> OBJECT_CODEC = AliasingMapCodec.wrap(
        RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("background").forGetter(OriginGui::background),
            ResourceLocation.CODEC.optionalFieldOf("border").forGetter(OriginGui::border),
            ResourceLocation.CODEC.optionalFieldOf("name_plate").forGetter(OriginGui::namePlate),
            ResourceLocation.CODEC.optionalFieldOf("scroll_bar").forGetter(OriginGui::scrollBar),
            ResourceLocation.CODEC.optionalFieldOf("impact").forGetter(OriginGui::impact),
            TextComponent.CODEC.optionalFieldOf("impact_name").forGetter(OriginGui::impactName)
        ).apply(i, OriginGui::new)),
        Map.of("nameplate", "name_plate", "scrollbar", "scroll_bar"));

    public static final Codec<OriginGui> CODEC = Codec.either(ResourceLocation.CODEC, OBJECT_CODEC.codec()).xmap(
        either -> either.map(OriginGui::theme, Function.identity()),
        Either::right);

    public static OriginGui theme(ResourceLocation theme) {
        return new OriginGui(Optional.of(theme), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    }

    public static boolean isImage(ResourceLocation id) {
        return id.getPath().endsWith(IMAGE);
    }

    public Optional<ResourceLocation> themeFolder() {
        return background.filter(id -> !isImage(id));
    }

    public Optional<ResourceLocation> backgroundImage() {
        return background.filter(OriginGui::isImage);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeOptional(background, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(border, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(namePlate, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(scrollBar, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(impact, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(impactName.flatMap(OriginGui::toJson), FriendlyByteBuf::writeUtf);
    }

    public static OriginGui read(FriendlyByteBuf buf) {
        return new OriginGui(
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readOptional(FriendlyByteBuf::readUtf).flatMap(OriginGui::fromJson));
    }

    public static void writeOptional(FriendlyByteBuf buf, Optional<OriginGui> gui) {
        buf.writeBoolean(gui.isPresent());
        gui.ifPresent(present -> present.write(buf));
    }

    public static Optional<OriginGui> readOptional(FriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(read(buf)) : Optional.empty();
    }

    private static Optional<String> toJson(Component component) {
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).result().map(Object::toString);
    }

    private static Optional<Component> fromJson(String json) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result();
    }
}
