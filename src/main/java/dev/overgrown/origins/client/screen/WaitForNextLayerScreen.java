package dev.overgrown.origins.client.screen;

import dev.overgrown.origins.origin.OriginLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public final class WaitForNextLayerScreen extends Screen {

    private final ArrayList<OriginLayer> layerList;
    private final int currentLayerIndex;
    private final boolean showDirtBackground;
    private final boolean fromOrb;

    public WaitForNextLayerScreen(ArrayList<OriginLayer> layerList, int currentLayerIndex,
                                  boolean showDirtBackground, boolean fromOrb) {
        super(Component.empty());
        this.layerList = layerList;
        this.currentLayerIndex = currentLayerIndex;
        this.showDirtBackground = showDirtBackground;
        this.fromOrb = fromOrb;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        if (showDirtBackground) {
            this.renderDirtBackground(graphics);
        } else {
            super.renderBackground(graphics);
        }
    }
}
