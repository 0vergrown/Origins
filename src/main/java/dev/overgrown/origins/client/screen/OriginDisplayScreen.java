package dev.overgrown.origins.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.Badge;
import dev.overgrown.origins.client.BadgeClientState;
import dev.overgrown.origins.client.Season;
import dev.overgrown.origins.origin.Impact;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Environment(EnvType.CLIENT)
public abstract class OriginDisplayScreen extends Screen {

    protected static final int windowWidth = 176;
    protected static final int windowHeight = 182;

    private GuiTextures tex = GuiTextures.of(Season.DEFAULT);

    private record GuiTextures(ResourceLocation background, ResourceLocation border,
                               ResourceLocation namePlate, ResourceLocation slot, ResourceLocation handle,
                               ResourceLocation pressed, ResourceLocation[] impact) {
        static GuiTextures of(Season season) {
            String base = "textures/gui/choose_origin/" + season.dir() + "/";
            return new GuiTextures(
                Origins.id(base + "background.png"),
                Origins.id(base + "border.png"),
                Origins.id(base + "name_plate.png"),
                Origins.id(base + "scroll_bar/slot.png"),
                Origins.id(base + "scroll_bar.png"),
                Origins.id(base + "scroll_bar/pressed.png"),
                new ResourceLocation[]{
                    Origins.id(base + "impact/none.png"),
                    Origins.id(base + "impact/low.png"),
                    Origins.id(base + "impact/medium.png"),
                    Origins.id(base + "impact/high.png"),
                });
        }
    }

    protected int guiLeft;
    protected int guiTop;

    private Origin origin;
    private OriginLayer layer;
    private boolean isOriginRandom;
    private List<Component> randomOriginText = List.of();

    protected int scrollPos = 0;
    private int currentMaxScroll = 0;

    private Badge hoveredBadge;
    private ResourceLocation hoveredBadgePowerId;

    private boolean scrolling = false;
    private int scrollDragStart = 0;
    private double mouseDragStart = 0;

    protected final boolean showDirtBackground;

    protected OriginDisplayScreen(Component title, boolean showDirtBackground) {
        super(title);
        this.showDirtBackground = showDirtBackground;
    }

    public void showOrigin(@Nullable Origin origin, @Nullable OriginLayer layer, boolean isRandom) {
        this.origin = origin;
        this.layer = layer;
        this.isOriginRandom = isRandom;
        this.scrollPos = 0;
    }

    public void setRandomOriginText(List<Component> names) {
        this.randomOriginText = names == null ? List.of() : names;
    }

    public @Nullable Origin getCurrentOrigin() {
        return origin;
    }

    public @Nullable OriginLayer getCurrentLayer() {
        return layer;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - windowWidth) / 2;
        this.guiTop = (this.height - windowHeight) / 2;
        this.tex = GuiTextures.of(Season.current());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (showDirtBackground) {
            this.renderMenuBackground(graphics);
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderOriginWindow(graphics, mouseX, mouseY);
        if (origin != null) {
            renderScrollbar(graphics, mouseX, mouseY);
        }
        if (hoveredBadge != null && hoveredBadge.hasTooltip()) {
            int widthLimit = Math.max(120, this.width - mouseX - 24);
            hoveredBadge.renderTooltip(graphics, this.font, mouseX, mouseY, widthLimit, hoveredBadgePowerId, partialTick);
        }
    }

    protected void blitWindowBackground(GuiGraphics graphics) {
        beginBlit();
        graphics.blit(tex.background(), guiLeft, guiTop, 0.0F, 0.0F, windowWidth, windowHeight, windowWidth, windowHeight);
    }

    protected void blitWindowBorder(GuiGraphics graphics) {
        beginBlit();
        graphics.blit(tex.border(), guiLeft, guiTop, 0.0F, 0.0F, windowWidth, windowHeight, windowWidth, windowHeight);
    }

    private void renderOriginWindow(GuiGraphics graphics, int mouseX, int mouseY) {
        blitWindowBackground(graphics);
        if (origin != null) {
            renderOriginContent(graphics, mouseX, mouseY);
        }
        blitWindowBorder(graphics);
        if (origin != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 5);
            renderOriginHeader(graphics);
            renderImpactDots(graphics, mouseX, mouseY);
            graphics.pose().popPose();
            graphics.drawCenteredString(this.font, getTitleText().getString(),
                this.width / 2, this.guiTop - 15, 0xFFFFFF);
        }
        RenderSystem.disableBlend();
    }

    static void beginBlit() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderOriginHeader(GuiGraphics graphics) {
        beginBlit();
        graphics.blit(tex.namePlate(), guiLeft + 10, guiTop + 10, 0.0F, 0.0F, 150, 26, 150, 26);
        dev.overgrown.apoli.client.IconRenderer.render(graphics, origin.icon(), guiLeft + 15, guiTop + 15);
        drawScrollingName(graphics, origin.name(), guiLeft + 39, guiLeft + 124, guiTop + 19, 0xFFFFFF, origin.nameScrollSpeed());
    }

    private void drawScrollingName(GuiGraphics graphics, Component text, int minX, int maxX, int y, int color, float speedMultiplier) {
        int boxWidth = maxX - minX;
        int textWidth = font.width(text);
        if (textWidth <= boxWidth) {
            graphics.drawString(font, text, minX, y, color, true);
            return;
        }
        int scroll = 0;
        if (speedMultiplier > 0f) {
            double overflow = textWidth - boxWidth;
            double time = net.minecraft.Util.getMillis() / 1000.0;
            double period = Math.max(overflow * 0.5, 3.0) / speedMultiplier;
            double phase = Math.sin((Math.PI / 2.0) * Math.cos((Math.PI * 2.0) * time / period)) / 2.0 + 0.5;
            scroll = (int) net.minecraft.util.Mth.lerp(phase, 0.0, textWidth - boxWidth);
        }
        graphics.enableScissor(minX, y - 2, maxX, y + 11);
        graphics.drawString(font, text, minX - scroll, y, color, true);
        graphics.disableScissor();
    }

    private void renderImpactDots(GuiGraphics graphics, int mouseX, int mouseY) {
        Impact impact = origin.impact();
        int level = impact.level();
        ResourceLocation sprite = tex.impact()[Math.max(0, Math.min(level, tex.impact().length - 1))];
        beginBlit();
        graphics.blit(sprite, guiLeft + 128, guiTop + 19, 0.0F, 0.0F, 28, 8, 28, 8);
        if (mouseX >= guiLeft + 128 && mouseX <= guiLeft + 158
            && mouseY >= guiTop + 19 && mouseY <= guiTop + 27) {
            MutableComponent tip = Component.translatable("origins.gui.impact.impact")
                .append(": ")
                .append(impactName(impact).withStyle(impactColor(impact)));
            graphics.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    private void renderOriginContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = guiLeft + 18;
        int textWidth = windowWidth - 48;
        int y = guiTop + 50;
        int startY = y;
        int endY = y - 72 + windowHeight;
        y -= scrollPos;

        hoveredBadge = null;
        hoveredBadgePowerId = null;

        Component description = origin.description();
        List<FormattedCharSequence> descLines = font.split(description, textWidth);
        for (FormattedCharSequence line : descLines) {
            if (y >= startY - 18 && y <= endY + 12) {
                graphics.drawString(font, line, x + 2, y - 6, 0xCCCCCC, false);
            }
            y += 12;
        }

        if (isOriginRandom) {
            for (Component name : randomOriginText) {
                for (FormattedCharSequence line : font.split(name, textWidth)) {
                    y += 12;
                    if (y >= startY - 24 && y <= endY + 12) {
                        graphics.drawString(font, line, x + 2, y, 0xCCCCCC, false);
                    }
                }
            }
            y += 14;
        } else {
            Player viewer = this.minecraft.player;
            List<ResourceLocation> shownPowers = viewer != null ? origin.powersFor(viewer) : origin.powers();
            for (ResourceLocation powerId : shownPowers) {
                Power power = ApoliPowers.get(powerId);
                if (power == null || power.hidden()) continue;
                MutableComponent underlined = Component.empty().withStyle(ChatFormatting.UNDERLINE)
                    .append(power.displayName(powerId).copy());
                List<FormattedCharSequence> nameLines = font.split(underlined, textWidth);
                int lastNameWidth = 0;
                for (int li = 0; li < nameLines.size(); li++) {
                    FormattedCharSequence nameLine = nameLines.get(li);
                    if (y >= startY - 24 && y <= endY + 12) {
                        graphics.drawString(font, nameLine, x, y, 0xFFFFFF, false);
                    }
                    lastNameWidth = font.width(nameLine);
                    if (li < nameLines.size() - 1) y += 12;
                }

                int extraBadgeRows = drawBadges(graphics, BadgeClientState.get(powerId), powerId,
                    x, y, lastNameWidth, mouseX, mouseY, startY, endY);
                y += extraBadgeRows * 10;

                Component descComp = power.displayDescription(powerId);
                for (FormattedCharSequence line : font.split(descComp, textWidth)) {
                    y += 12;
                    if (y >= startY - 24 && y <= endY + 12) {
                        graphics.drawString(font, line, x + 2, y, 0xCCCCCC, false);
                    }
                }
                y += 14;
            }
        }
        y += scrollPos;
        currentMaxScroll = y - 14 - (guiTop + 158);
        if (currentMaxScroll < 0) {
            currentMaxScroll = 0;
        }
    }

    private int drawBadges(GuiGraphics graphics, List<Badge> badges, ResourceLocation powerId,
                           int x, int nameLineY, int nameWidth, int mouseX, int mouseY, int startY, int endY) {
        if (badges.isEmpty()) return 0;
        int step = 10;
        int badgeRight = guiLeft + windowWidth - 26;
        int rowStartX = x + nameWidth + 4;
        int offX = 0;
        int offY = 0;
        for (Badge badge : badges) {
            int bx = rowStartX + offX * step;
            int by = nameLineY - 1 + offY * step;
            if (bx + 9 > badgeRight) {
                offX = 0;
                offY++;
                rowStartX = x;
                bx = rowStartX;
                by = nameLineY - 1 + offY * step;
            }
            if (by >= startY - 12 && by <= endY + 12) {
                int spriteSize = BadgeClientState.spriteSize(badge.spriteId());
                beginBlit();
                graphics.blit(badge.spriteId(), bx, by, 9, 9, 0.0F, 0.0F, spriteSize, spriteSize, spriteSize, spriteSize);
                if (badge.hasTooltip()
                    && mouseX >= bx && mouseX < bx + 9 && mouseY >= by && mouseY < by + 9) {
                    hoveredBadge = badge;
                    hoveredBadgePowerId = powerId;
                }
            }
            offX++;
        }
        return offY;
    }

    protected void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!canScroll()) return;
        int handleY = 36;
        int maxHandleY = 141;
        float part = scrollPos / (float) currentMaxScroll;
        handleY += (int) ((maxHandleY - handleY) * part);
        boolean active = scrolling
            || (mouseX >= guiLeft + 156 && mouseX < guiLeft + 156 + 6
                && mouseY >= guiTop + handleY && mouseY < guiTop + handleY + 27);
        beginBlit();
        graphics.blit(tex.slot(), guiLeft + 155, guiTop + 35, 0.0F, 0.0F, 8, 134, 8, 134);
        graphics.blit(active ? tex.pressed() : tex.handle(), guiLeft + 156, guiTop + handleY, 0.0F, 0.0F, 6, 27, 6, 27);
    }

    protected boolean hasScrollableContent() {
        return origin != null;
    }

    protected void setMaxScroll(int max) {
        this.currentMaxScroll = Math.max(0, max);
        if (this.scrollPos > this.currentMaxScroll) this.scrollPos = this.currentMaxScroll;
    }

    protected boolean canScroll() {
        return hasScrollableContent() && currentMaxScroll > 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (canScroll()) {
            int handleY = 36;
            int maxHandleY = 141;
            float part = scrollPos / (float) currentMaxScroll;
            handleY += (int) ((maxHandleY - handleY) * part);
            if (mouseX >= guiLeft + 156 && mouseX < guiLeft + 156 + 6
                && mouseY >= guiTop + handleY && mouseY < guiTop + handleY + 27) {
                scrolling = true;
                scrollDragStart = handleY;
                mouseDragStart = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (scrolling) {
            int delta = (int) (mouseY - mouseDragStart);
            int newHandleY = Math.max(36, Math.min(141, scrollDragStart + delta));
            float part = (newHandleY - 36) / (float) (141 - 36);
            scrollPos = (int) (part * currentMaxScroll);
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean ret = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        int next = this.scrollPos - (int) scrollY * 4;
        this.scrollPos = next < 0 ? 0 : Math.min(next, this.currentMaxScroll);
        return ret;
    }

    protected Component getTitleText() {
        return Component.literal("Origins");
    }

    private static MutableComponent impactName(Impact impact) {
        return switch (impact) {
            case NONE -> Component.translatable("origins.gui.impact.none");
            case LOW -> Component.translatable("origins.gui.impact.low");
            case MEDIUM -> Component.translatable("origins.gui.impact.medium");
            case HIGH -> Component.translatable("origins.gui.impact.high");
        };
    }

    private static ChatFormatting impactColor(Impact impact) {
        return switch (impact) {
            case NONE -> ChatFormatting.GRAY;
            case LOW -> ChatFormatting.GREEN;
            case MEDIUM -> ChatFormatting.YELLOW;
            case HIGH -> ChatFormatting.RED;
        };
    }

    protected Font fontRef() {
        return this.font;
    }
}
