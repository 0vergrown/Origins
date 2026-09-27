package dev.overgrown.origins.client.tooltip;

import dev.overgrown.apoli.client.TooltipRenderer;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.CraftingRecipeBadge;
import dev.overgrown.origins.badge.CraftingRecipeTooltipData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class CraftingRecipeClientTooltip implements ClientTooltipComponent {

    private static final ResourceLocation TEXTURE = Origins.id("textures/gui/tooltip/recipe_tooltip.png");

    private final CraftingRecipeTooltipData data;

    public CraftingRecipeClientTooltip(CraftingRecipeTooltipData data) {
        this.data = data;
    }

    public static void renderBadge(GuiGraphics graphics, Font font, CraftingRecipeBadge badge,
                                   int mouseX, int mouseY, int widthLimit) {
        List<ClientTooltipComponent> components = new ArrayList<>();
        badge.prefix().ifPresent(text -> addLines(components, font, text, widthLimit));
        if (!badge.output().isEmpty()) {
            components.add(ClientTooltipComponent.create(
                new CraftingRecipeTooltipData(badge.inputs(), badge.output(), badge.width())));
        }
        badge.suffix().ifPresent(text -> addLines(components, font, text, widthLimit));
        TooltipRenderer.render(graphics, font, components, mouseX, mouseY);
    }

    private static void addLines(List<ClientTooltipComponent> out, Font font, Component text, int widthLimit) {
        for (FormattedCharSequence line : font.split(text, widthLimit)) {
            out.add(ClientTooltipComponent.create(line));
        }
    }

    @Override
    public int getHeight() {
        return 68;
    }

    @Override
    public int getWidth(Font font) {
        return 130;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, 130, 86, 256, 256);
        int width = data.width();
        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                int index = column + row * width;
                int slotX = x + 8 + column * 18;
                int slotY = y + 8 + row * 18;
                ItemStack stack = (column >= width || index >= data.inputs().size())
                    ? ItemStack.EMPTY
                    : data.inputs().get(index);
                graphics.renderItem(stack, slotX, slotY);
                graphics.renderItemDecorations(font, stack, slotX, slotY);
            }
        }
        graphics.renderItem(data.output(), x + 101, y + 25);
        graphics.renderItemDecorations(font, data.output(), x + 101, y + 25);
    }
}
