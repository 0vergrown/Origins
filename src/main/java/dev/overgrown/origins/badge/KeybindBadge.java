package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import dev.overgrown.apoli.client.ApoliKeyMappings;
import dev.overgrown.apoli.data.Key;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;


public record KeybindBadge(ResourceLocation spriteId, String text, String keyId) implements Badge {

    public static final MapCodec<KeybindBadge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("sprite").forGetter(KeybindBadge::spriteId),
        Codec.STRING.fieldOf("text").forGetter(KeybindBadge::text),
        Codec.STRING.optionalFieldOf("key", Key.PRIMARY_ACTIVE).forGetter(KeybindBadge::keyId)
    ).apply(i, KeybindBadge::new));

    @Override
    public ResourceLocation typeId() {
        return BadgeTypes.KEYBIND.id();
    }

    @Override
    public boolean hasTooltip() {
        return text != null && !text.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                              ResourceLocation powerId, float time) {
        if (!hasTooltip()) return;
        KeyMapping mapping = ApoliKeyMappings.resolve(keyId);
        Component keyName = mapping != null
            ? mapping.getTranslatedKeyMessage()
            : Component.translatable("origins.gui.badge.unbound");
        Component bracketed = Component.literal("[").append(keyName).append("]");
        Component line = Component.translatable(text, bracketed);
        List<FormattedCharSequence> lines = font.split(line, widthLimit);
        if (lines.isEmpty()) return;
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(spriteId);
        buf.writeUtf(text);
        buf.writeUtf(keyId);
    }

    public static KeybindBadge fromNetwork(RegistryFriendlyByteBuf buf) {
        ResourceLocation sprite = buf.readResourceLocation();
        String text = buf.readUtf();
        String keyId = buf.readUtf();
        return new KeybindBadge(sprite, text, keyId);
    }
}
