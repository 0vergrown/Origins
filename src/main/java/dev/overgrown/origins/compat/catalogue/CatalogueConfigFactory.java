package dev.overgrown.origins.compat.catalogue;

import dev.overgrown.origins.client.config.OriginsConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.Screen;

@Environment(EnvType.CLIENT)
public final class CatalogueConfigFactory {

    private CatalogueConfigFactory() {}

    public static Screen createConfigScreen(Screen currentScreen, ModContainer container) {
        return new OriginsConfigScreen(currentScreen);
    }
}
