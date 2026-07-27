package dev.overgrown.origins.client.screen;

import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class OriginRollScreen extends Screen {

    private final OriginLayer layer;
    private final ResourceLocation resultId;
    private final int spinDuration;
    private final List<ResourceLocation> pool = new ArrayList<>();
    private final RandomSource random = RandomSource.create();

    private int ticks;
    private int sinceSwap;
    private int poolIndex;
    private boolean landed;
    private int landedTicks;

    public OriginRollScreen(OriginLayer layer, ResourceLocation resultId, int spinDuration) {
        super(Component.translatable("screen.origins.roll"));
        this.layer = layer;
        this.resultId = resultId;
        this.spinDuration = Math.max(20, spinDuration);
        for (ResourceLocation id : layer.allOrigins()) {
            if (layer.excludedFromRandom().contains(id)) continue;
            Origin origin = OriginRegistry.get(id);
            if (origin == null) continue;
            if (!origin.choosable() && !layer.randomAllowsUnchoosable()) continue;
            pool.add(id);
        }
        if (pool.isEmpty()) pool.add(resultId);
        this.poolIndex = random.nextInt(pool.size());
    }

    @Override
    public void tick() {
        if (landed) {
            landedTicks++;
            if (landedTicks > 100) onClose();
            return;
        }
        ticks++;
        if (ticks >= spinDuration) {
            land();
            return;
        }
        if (++sinceSwap >= swapInterval()) {
            sinceSwap = 0;
            if (pool.size() > 1) {
                poolIndex = (poolIndex + 1 + random.nextInt(pool.size() - 1)) % pool.size();
            }
            float progress = (float) ticks / spinDuration;
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f + progress * 0.8f));
            }
        }
    }

    private int swapInterval() {
        float p = (float) ticks / spinDuration;
        return 1 + (int) (9 * p * p);
    }

    private void land() {
        landed = true;
        landedTicks = 0;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);

        int cx = this.width / 2;
        int cy = this.height / 2;

        graphics.drawCenteredString(this.font, layer.name(), cx, cy - 70, 0xA0A0A0);

        Origin shown = OriginRegistry.get(landed ? resultId : pool.get(poolIndex));
        if (shown == null) return;

        graphics.pose().pushPose();
        graphics.pose().translate(cx - 16, cy - 52, 0);
        graphics.pose().scale(2.0f, 2.0f, 1.0f);
        graphics.renderFakeItem(shown.icon(), 0, 0);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy + 4, 0);
        graphics.pose().scale(2.0f, 2.0f, 1.0f);
        graphics.drawCenteredString(this.font, shown.name(), 0, 0, 0xFFFFFF);
        graphics.pose().popPose();

        if (landed) {
            graphics.drawCenteredString(this.font,
                Component.translatable("screen.origins.roll.obtained"), cx, cy + 28, 0xFFD700);
            if (landedTicks > 5) {
                graphics.drawCenteredString(this.font,
                    Component.translatable("screen.origins.roll.continue"), cx, cy + 44, 0x808080);
            }
        } else {
            graphics.drawCenteredString(this.font,
                Component.translatable("screen.origins.roll.rolling"), cx, cy + 28, 0xA0A0A0);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (landed && landedTicks > 5) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!landed) {
                land();
            } else {
                onClose();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
