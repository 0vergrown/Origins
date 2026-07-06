package dev.overgrown.origins.client.screen;

import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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

@Environment(EnvType.CLIENT)
public final class ViewOriginScreen extends OriginDisplayScreen {

    private final ArrayList<Tuple<OriginLayer, Origin>> originLayers;
    private int currentLayer = 0;
    private Button chooseOriginButton;

    public ViewOriginScreen() {
        super(Component.translatable("origins.screen.view_origin"), false);
        Player player = Minecraft.getInstance().player;
        this.originLayers = new ArrayList<>();
        if (player != null) {
            Map<ResourceLocation, ResourceLocation> picks = OriginsClientState.get(player.getUUID());
            for (OriginLayer layer : OriginLayers.enabledOrdered()) {
                if (layer.hidden()) continue;
                ResourceLocation chosenId = picks.getOrDefault(layer.id(), OriginRegistry.EMPTY_ID);
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
        if (!originLayers.isEmpty()) {
            Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
            showOrigin(current.getB(), current.getA(), false);
        } else {
            showOrigin(null, null, false);
        }
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
                }).bounds(guiLeft - 40, this.height / 2 - 10, 20, 20).build());
                addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                    currentLayer = (currentLayer + 1) % originLayers.size();
                    Tuple<OriginLayer, Origin> current = originLayers.get(currentLayer);
                    showOrigin(current.getB(), current.getA(), false);
                    refreshChooseButton();
                }).bounds(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("origins.gui.close"), b ->
            Minecraft.getInstance().setScreen(null))
            .bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20).build());
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
