package dev.overgrown.origins.client.screen;

import dev.overgrown.origins.item.OriginsItems;
import dev.overgrown.origins.network.OriginsClientNetwork;
import dev.overgrown.origins.origin.Impact;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ChooseOriginScreen extends OriginDisplayScreen {

    private final ArrayList<OriginLayer> layerList;
    private final int currentLayerIndex;
    private final List<Origin> originSelection;
    private int currentOrigin = 0;
    private int maxSelection;
    private Origin randomOrigin;
    private boolean fromOrb;

    public ChooseOriginScreen(ArrayList<OriginLayer> layerList, int currentLayerIndex,
                              boolean showDirtBackground, boolean fromOrb) {
        super(Component.translatable("origins.screen.choose_origin"), showDirtBackground);
        this.layerList = layerList;
        this.currentLayerIndex = currentLayerIndex;
        this.fromOrb = fromOrb;
        this.originSelection = new ArrayList<>(10);

        Player player = Minecraft.getInstance().player;
        OriginLayer currentLayer = layerList.get(currentLayerIndex);
        if (player != null) {
            for (ResourceLocation id : currentLayer.availableOrigins(player)) {
                Origin origin = OriginRegistry.get(id);
                if (origin != null && origin.choosable()) {
                    originSelection.add(origin);
                }
            }
        }
        originSelection.sort(Comparator
            .comparingInt((Origin o) -> o.impact().level())
            .thenComparingInt(Origin::order));
        maxSelection = originSelection.size();
        boolean randomRollable = currentLayer.allowRandom() && player != null
            && (!originSelection.isEmpty()
                || (currentLayer.randomAllowsUnchoosable() && !currentLayer.availableOrigins(player).isEmpty()));
        if (randomRollable) {
            maxSelection += 1;
        }
        if (maxSelection > 0) {
            Origin first = getCurrentOriginInternal();
            showOrigin(first, layerList.get(currentLayerIndex), first == randomOrigin);
        }
    }

    public ChooseOriginScreen(OriginLayer layer, boolean fromOrb) {
        this(singletonList(layer), 0, false, fromOrb);
    }

    private static ArrayList<OriginLayer> singletonList(OriginLayer layer) {
        ArrayList<OriginLayer> list = new ArrayList<>(1);
        list.add(layer);
        return list;
    }

    private void openNextLayerScreen() {
        Minecraft.getInstance().setScreen(new WaitForNextLayerScreen(layerList, currentLayerIndex,
            this.showDirtBackground, this.fromOrb));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (maxSelection > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                currentOrigin = (currentOrigin - 1 + maxSelection) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }).bounds(guiLeft - 40, this.height / 2 - 10, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                currentOrigin = (currentOrigin + 1) % maxSelection;
                Origin newOrigin = getCurrentOriginInternal();
                showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin == randomOrigin);
            }).bounds(guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("origins.gui.select"), b -> {
            if (originSelection.isEmpty() && randomOrigin == null) return;
            OriginLayer currentLayer = layerList.get(currentLayerIndex);
            Origin chosen = getCurrentOriginInternal();

            ResourceLocation originId = chosen == randomOrigin
                ? new ResourceLocation("origins", "random")
                : chosen.id();
            OriginsClientNetwork.sendChoose(currentLayer.id(), originId, fromOrb);
            openNextLayerScreen();
        }).bounds(guiLeft + windowWidth / 2 - 50, guiTop + windowHeight + 5, 100, 20).build());
    }

    @Override
    protected Component getTitleText() {
        OriginLayer layer = getCurrentLayer();
        if (layer == null) return super.getTitleText();
        if (!layer.chooseTitleKey().isEmpty()) {
            return layer.chooseTitle();
        }
        return Component.translatable("origins.gui.choose_origin.title", layer.name());
    }

    private Origin getCurrentOriginInternal() {
        if (currentOrigin == originSelection.size()) {
            if (randomOrigin == null) initRandomOrigin();
            return randomOrigin;
        }
        return originSelection.get(currentOrigin);
    }

    private void initRandomOrigin() {
        ResourceLocation randomId = new ResourceLocation("origins", "random");
        this.randomOrigin = new Origin(
            randomId,
            java.util.Collections.emptyList(),
            dev.overgrown.origins.origin.OriginIcon.ofItem(new ItemStack(OriginsItems.ORB_OF_ORIGIN)),
            Impact.NONE,
            -1,
            Integer.MAX_VALUE,
            false,
            true,
            java.util.Optional.empty(),
            java.util.Optional.empty()
        );
        Player player = Minecraft.getInstance().player;
        List<Component> names = new ArrayList<>();
        if (player != null) {
            List<ResourceLocation> randoms = new ArrayList<>(
                layerList.get(currentLayerIndex).availableOrigins(player));
            randoms.removeIf(id -> {
                Origin o = OriginRegistry.get(id);
                return o == null || !o.choosable();
            });
            randoms.sort((ia, ib) -> {
                Origin a = OriginRegistry.get(ia);
                Origin b = OriginRegistry.get(ib);
                int impDelta = a.impact().level() - b.impact().level();
                return impDelta == 0 ? Integer.compare(a.order(), b.order()) : impDelta;
            });
            for (ResourceLocation id : randoms) {
                Origin o = OriginRegistry.get(id);
                if (o == null) continue;
                names.add(o.name());
            }
        }
        setRandomOriginText(names);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (maxSelection == 0) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
