package dev.overgrown.origins.origin;

import dev.overgrown.apoli.attribution.PowerCause;
import dev.overgrown.apoli.power.PowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class OriginCauseDescriber implements PowerCause.Describer {

    private OriginCauseDescriber() {}

    public static void register() {
        if (!PowerCause.ACTIVE) return;
        PowerCause.addDescriber(new OriginCauseDescriber());
    }

    @Override
    public @Nullable String describe(Entity holder, ResourceLocation powerId) {
        if (!(holder instanceof Player player)) return null;
        PowerContainer container = PowerContainer.of(holder);
        if (container == null) return null;
        Set<ResourceLocation> sources = container.sourcesOf(powerId);
        if (sources.isEmpty()) return null;
        for (ResourceLocation source : sources) {
            ResourceLocation layerId = OriginManager.layerOfSource(source);
            if (layerId == null) continue;
            ResourceLocation originId = OriginView.activeOn(player, layerId);
            if (originId == null) continue;
            return originId.getPath() + "/" + powerId.getPath();
        }
        return null;
    }
}
