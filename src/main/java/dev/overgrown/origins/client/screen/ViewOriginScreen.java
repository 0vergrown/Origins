package dev.overgrown.origins.client.screen;

import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

public final class ViewOriginScreen extends OriginDisplayScreen {

    private final ArrayList<Tuple<OriginLayer, Origin>> originLayers;
    private int currentLayer = 0;
    private Button chooseOriginButton;
    private Button swapButton;

    public ViewOriginScreen() {
        super(Component.translatable("origins.screen.view_origin"), false);
        this.originLayers = new ArrayList<>();
        rebuild();
    }

    private void rebuild() {
        Player player = Minecraft.getInstance().player;
        originLayers.clear();
        if (player != null) {
            Map<ResourceLocation, ResourceLocation> picks = OriginsClientState.get(player.getUUID());
            Map<ResourceLocation, ResourceLocation> swaps = OriginsClientState.getSwaps(player.getUUID());
            for (OriginLayer layer : OriginLayers.enabledOrdered()) {
                if (layer.hidden() || layer.swappable()) continue;
                ResourceLocation mainId = picks.getOrDefault(layer.id(), OriginRegistry.EMPTY_ID);
                ResourceLocation chosenId = swaps.getOrDefault(layer.id(), mainId);
                Origin chosen = OriginRegistry.get(chosenId);
                boolean choosable = OriginManager.hasChoosableOrigins(player, layer);
                boolean isEmpty = chosen == null || chosenId.equals(OriginRegistry.EMPTY_ID);
                if (!isEmpty || choosable) {
                    if (chosen == null) chosen = OriginRegistry.getOrEmpty(chosenId);
                    originLayers.add(new Tuple<>(layer, chosen));
                }
            }
            originLayers.sort(Comparator.comparing(Tuple::getA));
        }
        if (currentLayer >= originLayers.size()) currentLayer = 0;
        if (!originLayers.isEmpty()) {
            Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
            showOrigin(current.getB(), current.getA(), false);
        } else {
            showOrigin(null, null, false);
        }
    }

    public void refresh() {
        rebuild();
        refreshChooseButton();
        refreshSwapButton();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        if (!originLayers.isEmpty()) {
            chooseOriginButton = addRenderableWidget(Button.builder(Component.translatable("origins.gui.choose"), b -> {
                ArrayList<OriginLayer> list = new ArrayList<>();
                list.add(originLayers.get(currentLayer).getA());
                Minecraft.getInstance().setScreen(new ChooseOriginScreen(list, 0, false, false));
            }).bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight - 40, 100, 20).build());

            refreshChooseButton();

            if (originLayers.size() > 1) {
                addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                    currentLayer = (currentLayer - 1 + originLayers.size()) % originLayers.size();
                    Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
                    showOrigin(current.getB(), current.getA(), false);
                    refreshChooseButton();
                    refreshSwapButton();
                }).bounds(guiLeft - 40, this.height / 2 - 10, 20, 20).build());
                addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    currentLayer = (currentLayer + 1) % originLayers.size();
                    Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
                    showOrigin(current.getB(), current.getA(), false);
                    refreshChooseButton();
                    refreshSwapButton();
                }).bounds(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("origins.gui.close"), b ->
            Minecraft.getInstance().setScreen(null))
            .bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20).build());

        swapButton = addRenderableWidget(Button.builder(Component.literal("S"), b -> {
            OriginLayer layer = originLayers.isEmpty() ? null : originLayers.get(currentLayer).getA();
            if (layer != null) Minecraft.getInstance().setScreen(new SwapOriginScreen(layer.id()));
        }).bounds(guiLeft + windowWidth / 2 + 55, guiTop + windowHeight + 5, 20, 20).build());
        swapButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("origins.gui.swap_origin.button")));
        refreshSwapButton();
    }

    private void refreshSwapButton() {
        Player player = Minecraft.getInstance().player;
        if (player == null || swapButton == null) return;
        boolean visible = false;
        if (!originLayers.isEmpty()) {
            ResourceLocation layerId = originLayers.get(currentLayer).getA().id();
            ResourceLocation main = OriginsClientState.get(player.getUUID())
                .getOrDefault(layerId, OriginRegistry.EMPTY_ID);
            visible = !SwapManager.pool(player, layerId, main,
                OriginsClientState.getPool(player.getUUID())).isEmpty();
        }
        swapButton.active = visible;
        swapButton.visible = visible;
    }

    private void refreshChooseButton() {
        Player player = Minecraft.getInstance().player;
        if (player == null || chooseOriginButton == null) return;
        Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
        boolean isEmptyOrigin = current.getB() == null
            || current.getB().id().equals(OriginRegistry.EMPTY_ID);
        boolean visible = isEmptyOrigin && OriginManager.hasChoosableOrigins(player, current.getA());
        chooseOriginButton.active = visible;
        chooseOriginButton.visible = visible;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (originLayers.isEmpty()) {
            graphics.drawCenteredString(this.font,
                Component.translatable("origins.gui.view_origin.empty"),
                this.width / 2, guiTop + 48, 0xFFFFFF);
        }
    }

    @Override
    protected Component getTitleText() {
        OriginLayer layer = getCurrentLayer();
        if (layer == null) return super.getTitleText();
        if (!layer.viewTitleKey().isEmpty()) {
            return layer.viewTitle();
        }
        return Component.translatable("origins.gui.view_origin.title", layer.name());
    }
}
