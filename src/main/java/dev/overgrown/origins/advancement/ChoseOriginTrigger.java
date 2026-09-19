package dev.overgrown.origins.advancement;

import com.google.gson.JsonObject;
import dev.overgrown.origins.Origins;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public final class ChoseOriginTrigger extends SimpleCriterionTrigger<ChoseOriginTrigger.TriggerInstance> {

    public static final ResourceLocation ID = Origins.id("chose_origin");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected TriggerInstance createInstance(JsonObject json, ContextAwarePredicate player,
                                             DeserializationContext context) {
        ResourceLocation origin = json.has("origin")
            ? new ResourceLocation(GsonHelper.getAsString(json, "origin")) : null;
        ResourceLocation layer = json.has("layer")
            ? new ResourceLocation(GsonHelper.getAsString(json, "layer")) : null;
        return new TriggerInstance(player, origin, layer);
    }

    public void trigger(ServerPlayer player, ResourceLocation layer, ResourceLocation origin) {
        this.trigger(player, instance -> instance.matches(layer, origin));
    }

    public static final class TriggerInstance extends AbstractCriterionTriggerInstance {
        private final @Nullable ResourceLocation origin;
        private final @Nullable ResourceLocation layer;

        public TriggerInstance(ContextAwarePredicate player, @Nullable ResourceLocation origin,
                               @Nullable ResourceLocation layer) {
            super(ID, player);
            this.origin = origin;
            this.layer = layer;
        }

        public boolean matches(ResourceLocation onLayer, ResourceLocation chosen) {
            if (layer != null && !layer.equals(onLayer)) return false;
            return origin == null || origin.equals(chosen);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            if (origin != null) json.addProperty("origin", origin.toString());
            if (layer != null) json.addProperty("layer", layer.toString());
            return json;
        }
    }
}
