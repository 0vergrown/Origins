package dev.overgrown.origins.client.config;

import dev.overgrown.origins.OriginsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public class OriginsConfigScreen extends Screen {

    private static final int WIDGET_WIDTH = 260;

    @Nullable
    private final Screen parent;

    public OriginsConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("origins.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - WIDGET_WIDTH / 2;
        int y = Math.max(50, this.height / 2 - 30);

        this.addRenderableWidget(CycleButton.<String>builder(OriginsConfigScreen::themeLabel)
            .withValues(OriginsConfig.GUI_THEMES)
            .withInitialValue(OriginsConfig.guiTheme())
            .withTooltip(value -> Tooltip.create(Component.translatable("origins.config.gui_theme." + value + ".tooltip")))
            .create(x, y, WIDGET_WIDTH, 20,
                Component.translatable("origins.config.gui_theme"),
                (button, value) -> OriginsConfig.setGuiTheme(value)));

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
            .bounds(this.width / 2 - 100, y + 32, 200, 20)
            .build());
    }

    private static Component themeLabel(String value) {
        return Component.translatable("origins.config.gui_theme." + value.toLowerCase(Locale.ROOT));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
