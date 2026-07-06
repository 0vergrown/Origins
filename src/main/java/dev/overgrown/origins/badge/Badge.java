package dev.overgrown.origins.badge;

import com.mojang.serialization.Codec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;


public interface Badge {

    
    Codec<Badge> CODEC = BadgeTypes.ID_CODEC.dispatch("type", Badge::typeId, BadgeTypes::codecFor);

    
    ResourceLocation spriteId();

    
    ResourceLocation typeId();

    
    boolean hasTooltip();

    
    @OnlyIn(Dist.CLIENT)
    void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                       ResourceLocation powerId, float time);

    
    void toNetwork(RegistryFriendlyByteBuf buf);

    
    static Badge fromNetwork(RegistryFriendlyByteBuf buf) {
        ResourceLocation typeId = buf.readResourceLocation();
        return BadgeTypes.readerFor(typeId).apply(buf);
    }

    
    static void writeNetwork(RegistryFriendlyByteBuf buf, Badge badge) {
        buf.writeResourceLocation(badge.typeId());
        badge.toNetwork(buf);
    }
}
