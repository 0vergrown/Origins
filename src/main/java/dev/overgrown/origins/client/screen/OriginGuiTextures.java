package dev.overgrown.origins.client.screen;

import dev.overgrown.apoli.client.TextureFallbacks;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.client.Season;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginGui;
import dev.overgrown.origins.origin.OriginLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public record OriginGuiTextures(ResourceLocation background, ResourceLocation border, ResourceLocation namePlate,
                                ResourceLocation slot, ResourceLocation handle, ResourceLocation pressed,
                                ResourceLocation[] impacts, Optional<Component> impactName) {
    private static final String ROOT = "textures/gui/choose_origin/";
    private static final String IMAGE = ".png";
    private static final String[] IMPACT_FILES = {"none", "low", "medium", "high"};

    public ResourceLocation impact(int level) {
        return impacts[Math.max(0, Math.min(level, impacts.length - 1))];
    }

    public static OriginGuiTextures resolve(@Nullable Origin origin, @Nullable OriginLayer layer, Season season) {
        OriginGui first = origin == null ? null : origin.gui().orElse(null);
        OriginGui second = layer == null ? null : layer.gui().orElse(null);
        String seasonal = ROOT + season.dir() + "/";
        ResourceLocation[] impacts = new ResourceLocation[IMPACT_FILES.length];
        for (int level = 0; level < impacts.length; level++) {
            String file = IMPACT_FILES[level] + IMAGE;
            impacts[level] = choose(first, second,
                gui -> gui.impact().map(id -> OriginGui.isImage(id) ? image(id) : inFolder(id, file)),
                "impact/" + file, Origins.id(seasonal + "impact/" + file));
        }
        Optional<Component> impactName = first != null && first.impactName().isPresent()
            ? first.impactName()
            : second == null ? Optional.empty() : second.impactName();
        return new OriginGuiTextures(
            choose(first, second, gui -> gui.backgroundImage().map(OriginGuiTextures::image), "background.png",
                Origins.id(seasonal + "background.png")),
            choose(first, second, gui -> gui.border().map(OriginGuiTextures::image), "border.png",
                Origins.id(seasonal + "border.png")),
            choose(first, second, gui -> gui.namePlate().map(OriginGuiTextures::image), "name_plate.png",
                Origins.id(seasonal + "name_plate.png")),
            choose(first, second,
                gui -> gui.scrollBar().filter(id -> !OriginGui.isImage(id)).map(id -> inFolder(id, "slot.png")),
                "scroll_bar/slot.png", Origins.id(seasonal + "scroll_bar/slot.png")),
            choose(first, second, gui -> gui.scrollBar().map(OriginGuiTextures::image), "scroll_bar.png",
                Origins.id(seasonal + "scroll_bar.png")),
            choose(first, second,
                gui -> gui.scrollBar().filter(id -> !OriginGui.isImage(id)).map(id -> inFolder(id, "pressed.png")),
                "scroll_bar/pressed.png", Origins.id(seasonal + "scroll_bar/pressed.png")),
            impacts,
            impactName);
    }

    private static ResourceLocation choose(@Nullable OriginGui first, @Nullable OriginGui second,
                                           Function<OriginGui, Optional<ResourceLocation>> explicit, String themeFile,
                                           ResourceLocation fallback) {
        ResourceLocation found = find(first, explicit, themeFile);
        if (found == null) found = find(second, explicit, themeFile);
        return found == null ? fallback : found;
    }

    @Nullable
    private static ResourceLocation find(@Nullable OriginGui gui, Function<OriginGui, Optional<ResourceLocation>> explicit,
                                         String themeFile) {
        if (gui == null) return null;
        Optional<ResourceLocation> direct = explicit.apply(gui);
        if (direct.isPresent() && TextureFallbacks.exists(direct.get())) return direct.get();
        Optional<ResourceLocation> theme = gui.themeFolder();
        if (theme.isPresent()) {
            ResourceLocation themed = inFolder(theme.get(), themeFile);
            if (TextureFallbacks.exists(themed)) return themed;
        }
        return null;
    }

    private static ResourceLocation image(ResourceLocation id) {
        String path = id.getPath();
        if (!path.endsWith(IMAGE)) path = path + IMAGE;
        if (!path.startsWith("textures/")) path = ROOT + path;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }

    private static ResourceLocation inFolder(ResourceLocation folder, String file) {
        String path = folder.getPath();
        if (path.endsWith(IMAGE)) path = path.substring(0, path.length() - IMAGE.length());
        if (!path.startsWith("textures/")) path = ROOT + path;
        return ResourceLocation.fromNamespaceAndPath(folder.getNamespace(), path + "/" + file);
    }
}
