package dev.overgrown.origins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class OriginsConfig {
    private static final String FILE = "origins.json";

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

    public static boolean seasonalGuiEnabled() {
        return "seasonal".equals(guiTheme());
    }

    private static synchronized void load() {
        if (loaded) return;
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE);
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
        switch (v) {
            case "seasonal":
            case "default":
            case "rainbow":
            case "frigid":
                return v;
            default:
                Origins.LOGGER.warn("[Origins] Unknown gui_theme '{}' — using 'seasonal'. Valid: seasonal, default, rainbow, frigid.", raw);
                return "seasonal";
        }
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
