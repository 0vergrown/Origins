package dev.overgrown.origins.advancement;

import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class OriginsCriteria {

    public static ChoseOriginTrigger CHOSE_ORIGIN;

    private OriginsCriteria() {}

    public static void register() {
        CHOSE_ORIGIN = CriteriaTriggers.register(new ChoseOriginTrigger());
    }

    public static void chose(ServerPlayer player, ResourceLocation layer, ResourceLocation origin) {
        if (CHOSE_ORIGIN != null && layer != null && origin != null) {
            CHOSE_ORIGIN.trigger(player, layer, origin);
        }
    }

    public static void refresh(ServerPlayer player) {
        if (CHOSE_ORIGIN == null) return;
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            ResourceLocation active = OriginView.activeOn(player, layer.id());
            if (active != null) CHOSE_ORIGIN.trigger(player, layer.id(), active);
        }
    }
}
