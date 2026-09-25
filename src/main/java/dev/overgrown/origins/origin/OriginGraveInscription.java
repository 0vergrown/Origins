package dev.overgrown.origins.origin;

import dev.overgrown.apoli.compat.grave.GraveInscriptions;
import dev.overgrown.origins.Origins;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class OriginGraveInscription {
    private static final Component SEPARATOR = Component.literal(" / ");

    private OriginGraveInscription() {}

    public static void register() {
        GraveInscriptions.register(Origins.id("origin"), OriginGraveInscription::inscribe);
    }

    private static @Nullable Component inscribe(ServerPlayer player) {
        MutableComponent line = null;
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.hidden()) continue;
            ResourceLocation originId = OriginView.activeOn(player, layer.id());
            if (originId == null) continue;
            Origin origin = OriginRegistry.get(originId);
            if (origin == null) continue;
            if (line == null) {
                line = Component.empty().append(origin.name());
            } else {
                line.append(SEPARATOR).append(origin.name());
            }
        }
        return line;
    }
}
