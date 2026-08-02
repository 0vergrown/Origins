package dev.overgrown.origins.client;

import dev.overgrown.origins.origin.OriginIcon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class OriginIconRenderer {

    private OriginIconRenderer() {}

    public static void render(GuiGraphics graphics, OriginIcon icon, int x, int y) {
        render(graphics, icon, x, y, 16, 16, false);
    }

    public static void renderFake(GuiGraphics graphics, OriginIcon icon, int x, int y) {
        render(graphics, icon, x, y, 16, 16, true);
    }

    public static void render(GuiGraphics graphics, OriginIcon icon, int x, int y, int width, int height,
                              boolean fake) {
        ResourceLocation texture = icon.texture().orElse(null);
        if (texture != null) {
            graphics.blit(texture, x, y, width, height, 0.0F, 0.0F, icon.width(), icon.height(),
                icon.width(), icon.height());
            return;
        }
        if (icon.stack().isEmpty()) return;
        if (fake) {
            graphics.renderFakeItem(icon.stack(), x, y);
        } else {
            graphics.renderItem(icon.stack(), x, y);
        }
    }
}
