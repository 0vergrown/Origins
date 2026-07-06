package dev.overgrown.origins.badge;

import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface Badge {

    Codec<Badge> CODEC = BadgeTypes.ID_CODEC.dispatch("type", Badge::typeId, BadgeTypes::codecFor);

    ResourceLocation spriteId();

    ResourceLocation typeId();

    boolean hasTooltip();

    @Environment(EnvType.CLIENT)
    void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                       ResourceLocation powerId, float time);

    void toNetwork(FriendlyByteBuf buf);

    static Badge fromNetwork(FriendlyByteBuf buf) {
        ResourceLocation typeId = buf.readResourceLocation();
        return BadgeTypes.readerFor(typeId).apply(buf);
    }

    static void writeNetwork(FriendlyByteBuf buf, Badge badge) {
        buf.writeResourceLocation(badge.typeId());
        badge.toNetwork(buf);
    }
}
