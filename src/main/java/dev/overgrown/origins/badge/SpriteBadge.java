package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SpriteBadge(ResourceLocation spriteId, List<ResourceLocation> fallback) implements Badge {

    public static final MapCodec<SpriteBadge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("sprite").forGetter(SpriteBadge::spriteId),
        BadgeFallback.CODEC.optionalFieldOf("fallback", List.of()).forGetter(SpriteBadge::fallback)
    ).apply(i, SpriteBadge::new));

    @Override
    public Badge withFallback(List<ResourceLocation> fallback) {
        return new SpriteBadge(spriteId, fallback);
    }

    @Override
    public ResourceLocation typeId() {
        return BadgeTypes.SPRITE.id();
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                              ResourceLocation powerId, float time) {}

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(spriteId);
    }

    public static SpriteBadge fromNetwork(RegistryFriendlyByteBuf buf) {
        return new SpriteBadge(buf.readResourceLocation(), List.of());
    }
}
