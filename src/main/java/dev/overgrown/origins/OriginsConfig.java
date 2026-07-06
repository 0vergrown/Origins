package dev.overgrown.origins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OriginsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "origins.json";
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
                String theme = "seasonal";
                boolean seasonal = true; 
                try (Reader r = Files.newBufferedReader(path)) {
                    JsonObject obj = GSON.fromJson(r, JsonObject.class);
                    if (obj != null) {
                        if (obj.has("gui_theme")) theme = normalize(obj.get("gui_theme").getAsString());
                        if (obj.has("seasonal_gui")) seasonal = obj.get("seasonal_gui").getAsBoolean();
                    }
                }
                
                guiTheme = !"seasonal".equals(theme) ? theme : (seasonal ? "seasonal" : "default");
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
            JsonObject obj = new JsonObject();
            obj.addProperty("gui_theme", guiTheme);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(obj, w);
            }
        } catch (Exception e) {
            Origins.LOGGER.warn("[Origins] Couldn't write default {} ({}).", FILE, e.toString());
        }
    }
}
