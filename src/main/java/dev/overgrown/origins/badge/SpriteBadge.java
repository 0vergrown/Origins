package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;


public record SpriteBadge(ResourceLocation spriteId) implements Badge {

    public static final MapCodec<SpriteBadge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("sprite").forGetter(SpriteBadge::spriteId)
    ).apply(i, SpriteBadge::new));

    @Override
    public ResourceLocation typeId() {
        return BadgeTypes.SPRITE.id();
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                              ResourceLocation powerId, float time) {
        
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(spriteId);
    }

    public static SpriteBadge fromNetwork(RegistryFriendlyByteBuf buf) {
        return new SpriteBadge(buf.readResourceLocation());
    }
}
