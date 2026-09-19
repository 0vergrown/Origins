package dev.overgrown.origins.advancement;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class OriginsCriteria {

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
        DeferredRegister.create(Registries.TRIGGER_TYPE, Origins.MOD_ID);

    public static final Supplier<ChoseOriginTrigger> CHOSE_ORIGIN =
        TRIGGERS.register("chose_origin", ChoseOriginTrigger::new);

    private OriginsCriteria() {}

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }

    public static void chose(ServerPlayer player, ResourceLocation layer, ResourceLocation origin) {
        if (layer != null && origin != null) CHOSE_ORIGIN.get().trigger(player, layer, origin);
    }

    public static void refresh(ServerPlayer player) {
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            ResourceLocation active = OriginView.activeOn(player, layer.id());
            if (active != null) CHOSE_ORIGIN.get().trigger(player, layer.id(), active);
        }
    }
}
