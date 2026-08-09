package dev.overgrown.origins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class OriginsConfig {
    private static final String FILE = "origins.json";

    public static final List<String> GUI_THEMES = List.of("seasonal", "default", "rainbow", "frigid");

    private record Data(String guiTheme, Optional<Boolean> seasonalGui) {
        static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("gui_theme", "seasonal").forGetter(Data::guiTheme),
            Codec.BOOL.optionalFieldOf("seasonal_gui").forGetter(Data::seasonalGui)
        ).apply(i, Data::new));
    }

    private static String guiTheme = "seasonal";
    private static boolean loaded = false;

    private OriginsConfig() {}

    public static String guiTheme() {
        if (!loaded) load();
        return guiTheme;
    }

    public static void setGuiTheme(String value) {
        if (!loaded) load();
        String theme = normalize(value);
        if (theme.equals(guiTheme)) return;
        guiTheme = theme;
        write(path());
    }

    public static boolean seasonalGuiEnabled() {
        return "seasonal".equals(guiTheme());
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    private static synchronized void load() {
        if (loaded) return;
        Path path = path();
        try {
            if (Files.exists(path)) {
                Data data = Data.CODEC.parse(JsonOps.INSTANCE, GsonHelper.parse(Files.readString(path)))
                    .resultOrPartial(err -> Origins.LOGGER.warn("[Origins] Invalid {}: {}", FILE, err))
                    .orElse(new Data("seasonal", Optional.empty()));
                String theme = normalize(data.guiTheme());
                guiTheme = !"seasonal".equals(theme) ? theme
                    : (data.seasonalGui().orElse(true) ? "seasonal" : "default");
            } else {
                write(path);
            }
        } catch (Exception e) {
            Origins.LOGGER.warn("[Origins] Couldn't read {} ({}); using defaults.", FILE, e.toString());
        }
        loaded = true;
    }

    private static String normalize(String raw) {
        String v = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (GUI_THEMES.contains(v)) return v;
        Origins.LOGGER.warn("[Origins] Unknown gui_theme '{}' — using 'seasonal'. Valid: {}.",
            raw, String.join(", ", GUI_THEMES));
        return "seasonal";
    }

    private static void write(Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Data.CODEC.encodeStart(JsonOps.INSTANCE, new Data(guiTheme, Optional.empty()))
                .resultOrPartial(err -> Origins.LOGGER.warn("[Origins] Couldn't encode default {} ({}).", FILE, err))
                .ifPresent(json -> {
                    try {
                        Files.writeString(path, GsonHelper.toStableString(json));
                    } catch (Exception e) {
                        Origins.LOGGER.warn("[Origins] Couldn't write default {} ({}).", FILE, e.toString());
                    }
                });
        } catch (Exception e) {
            Origins.LOGGER.warn("[Origins] Couldn't write default {} ({}).", FILE, e.toString());
        }
    }
}
