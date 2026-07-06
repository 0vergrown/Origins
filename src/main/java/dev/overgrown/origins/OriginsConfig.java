package dev.overgrown.origins;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;


public final class OriginsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SEASONAL_GUI = BUILDER
        .comment("Legacy toggle for the seasonal Origins GUI / Orb theme. Only applies when gui_theme = seasonal;",
                 "set gui_theme instead. false here forces the default texture.")
        .define("seasonal_gui", true);

    private static final ModConfigSpec.ConfigValue<String> GUI_THEME = BUILDER
        .comment("Origins GUI / Orb of Origin texture theme:",
                 "  seasonal  = auto by date (June rainbow, Dec 24-26 frigid, else default),",
                 "  default  /  rainbow  /  frigid  = always that texture.")
        .defineInList("gui_theme", "seasonal", List.of("seasonal", "default", "rainbow", "frigid"));

    public static final ModConfigSpec SPEC = BUILDER.build();

    private OriginsConfig() {}

    
    public static String guiTheme() {
        
        
        if (!SPEC.isLoaded()) return "seasonal";
        String theme = GUI_THEME.get();
        
        if (!"seasonal".equals(theme)) return theme;
        return SEASONAL_GUI.get() ? "seasonal" : "default";
    }

    
    public static boolean seasonalGuiEnabled() {
        return "seasonal".equals(guiTheme());
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
