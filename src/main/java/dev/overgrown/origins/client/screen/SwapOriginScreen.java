package dev.overgrown.origins.client.screen;

import dev.overgrown.apoli.client.IconRenderer;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.network.payload.SwapSelectC2S;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.SwapManager;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class SwapOriginScreen extends OriginDisplayScreen {

    private static final int COLUMNS = 4;
    private static final int CELL = 26;
    private static final int PITCH = 34;
    private static final int GRID_LEFT = 18;
    private static final int GRID_TOP = 42;
    private static final int GRID_BOTTOM = 164;

    private final ResourceLocation targetLayerId;
    private final List<ResourceLocation> entries = new ArrayList<>();
    private final List<Button> cells = new ArrayList<>();
    private ResourceLocation activeOrigin;
    private int hoveredIndex = -1;

    public SwapOriginScreen(ResourceLocation targetLayerId) {
        super(Component.translatable("origins.screen.swap_origin"), false);
        this.targetLayerId = targetLayerId;
    }

    public void refresh() {
        rebuild();
        repositionCells();
    }

    private void rebuild() {
        entries.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ResourceLocation main = OriginsClientState.get(player.getUUID())
            .getOrDefault(targetLayerId, OriginRegistry.EMPTY_ID);
        entries.add(main);
        entries.addAll(SwapManager.pool(player, targetLayerId, main,
            OriginsClientState.getPool(player.getUUID())));
        activeOrigin = OriginsClientState.getSwaps(player.getUUID()).get(targetLayerId);
        if (activeOrigin == null) activeOrigin = main;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
        cells.clear();
        for (int i = 0; i < entries.size(); i++) {
            int index = i;
            Button cell = Button.builder(Component.empty(), b -> select(index))
                .bounds(cellX(i), cellY(i), CELL, CELL)
                .build();
            cells.add(cell);
            addWidget(cell);
        }
        addRenderableWidget(Button.builder(Component.translatable("origins.gui.close"),
                b -> Minecraft.getInstance().setScreen(null))
            .bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20).build());
        setMaxScroll(gridHeight() - (GRID_BOTTOM - GRID_TOP));
        repositionCells();
    }

    private int rows() {
        return (entries.size() + COLUMNS - 1) / COLUMNS;
    }

    private int gridHeight() {
        return rows() <= 0 ? 0 : (rows() - 1) * PITCH + CELL;
    }

    private int cellX(int index) {
        return guiLeft + GRID_LEFT + (index % COLUMNS) * PITCH;
    }

    private int cellY(int index) {
        return guiTop + GRID_TOP + (index / COLUMNS) * PITCH - scrollPos;
    }

    private boolean onScreen(int y) {
        return y + CELL >= guiTop + GRID_TOP && y <= guiTop + GRID_BOTTOM;
    }

    private void select(int index) {
        if (index < 0 || index >= entries.size()) return;
        ResourceLocation pick = index == 0 ? SwapSelectC2S.MAIN : entries.get(index);
        PacketDistributor.sendToServer(new SwapSelectC2S(targetLayerId, pick));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected boolean hasScrollableContent() {
        return !entries.isEmpty();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderGrid(graphics, mouseX, mouseY);
        renderScrollbar(graphics, mouseX, mouseY);
        graphics.drawCenteredString(this.font, getTitleText().getString(),
            this.width / 2, this.guiTop - 15, 0xFFFFFF);
        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font,
                Component.translatable("origins.gui.swap_origin.empty"),
                this.width / 2, guiTop + 48, 0xFFFFFF);
        }
        if (hoveredIndex >= 0 && hoveredIndex < entries.size()) {
            Origin origin = OriginRegistry.get(entries.get(hoveredIndex));
            if (origin != null) graphics.renderTooltip(this.font, origin.name(), mouseX, mouseY);
        }
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        hoveredIndex = -1;
        graphics.enableScissor(guiLeft + 8, guiTop + GRID_TOP, guiLeft + windowWidth - 14, guiTop + GRID_BOTTOM);
        for (int i = 0; i < entries.size(); i++) {
            int x = cellX(i);
            int y = cellY(i);
            if (!onScreen(y)) continue;
            Origin origin = OriginRegistry.get(entries.get(i));
            if (origin == null) continue;

            boolean selected = entries.get(i).equals(activeOrigin);
            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL
                && mouseY >= guiTop + GRID_TOP && mouseY <= guiTop + GRID_BOTTOM;
            if (hovered) hoveredIndex = i;

            graphics.fill(x, y, x + CELL, y + CELL, hovered ? 0x60FFFFFF : 0x40000000);
            graphics.renderOutline(x, y, CELL, CELL, selected ? 0xFFFFFFFF : 0xFF3F3F3F);
            IconRenderer.render(graphics, origin.icon(), x + 5, y + 5);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        repositionCells();
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dx, dy);
        repositionCells();
        return handled;
    }

    private void repositionCells() {
        for (int i = 0; i < cells.size(); i++) {
            Button cell = cells.get(i);
            cell.setY(cellY(i));
            cell.active = onScreen(cell.getY());
        }
    }

    @Override
    protected Component getTitleText() {
        OriginLayer layer = OriginLayers.get(targetLayerId);
        if (layer == null) return Component.translatable("origins.gui.swap_origin.title.generic");
        return Component.translatable("origins.gui.swap_origin.title", layer.name());
    }
}
