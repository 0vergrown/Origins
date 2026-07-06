package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.TextComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public record TooltipBadge(ResourceLocation spriteId, Component text) implements Badge {

    public static final MapCodec<TooltipBadge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("sprite").forGetter(TooltipBadge::spriteId),
        TextComponent.CODEC.fieldOf("text").forGetter(TooltipBadge::text)
    ).apply(i, TooltipBadge::new));

    @Override
    public ResourceLocation typeId() {
        return BadgeTypes.TOOLTIP.id();
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                              ResourceLocation powerId, float time) {
        List<FormattedCharSequence> lines = font.split(text, widthLimit);
        if (lines.isEmpty()) return;
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(spriteId);
        ComponentSerialization.STREAM_CODEC.encode(buf, text);
    }

    public static TooltipBadge fromNetwork(RegistryFriendlyByteBuf buf) {
        ResourceLocation sprite = buf.readResourceLocation();
        Component text = ComponentSerialization.STREAM_CODEC.decode(buf);
        return new TooltipBadge(sprite, text);
    }
}
