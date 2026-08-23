package dev.overgrown.origins.origin;

import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OriginView {

    private OriginView() {}

    public static Map<ResourceLocation, ResourceLocation> chosen(Player player) {
        if (player.level().isClientSide()) return OriginsClientState.get(player.getUUID());
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? Map.of() : state.snapshot();
    }

    public static Map<ResourceLocation, ResourceLocation> swaps(Player player) {
        if (player.level().isClientSide()) return OriginsClientState.getSwaps(player.getUUID());
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? Map.of() : state.swapSnapshot();
    }

    public static List<ResourceLocation> pool(Player player) {
        List<ResourceLocation> out = new ArrayList<>();
        if (player.level().isClientSide()) {
            for (List<ResourceLocation> granted : OriginsClientState.getPool(player.getUUID()).values()) {
                for (ResourceLocation id : granted) {
                    if (!out.contains(id)) out.add(id);
                }
            }
            return out;
        }
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null) return out;
        for (var granted : state.poolSnapshot().values()) {
            for (ResourceLocation id : granted) {
                if (!out.contains(id)) out.add(id);
            }
        }
        return out;
    }

    public static @Nullable ResourceLocation activeOn(Player player, ResourceLocation layerId) {
        ResourceLocation swapped = swaps(player).get(layerId);
        if (swapped != null) return swapped;
        ResourceLocation main = chosen(player).get(layerId);
        return main == null || main.equals(OriginRegistry.EMPTY_ID) ? null : main;
    }

    public static boolean holds(Player player, ResourceLocation originId) {
        for (ResourceLocation held : chosen(player).values()) {
            if (originId.equals(held)) return true;
        }
        for (ResourceLocation held : swaps(player).values()) {
            if (originId.equals(held)) return true;
        }
        return false;
    }

    public static boolean inPool(Player player, ResourceLocation originId) {
        return pool(player).contains(originId);
    }

    public static int impactOn(Player player, ResourceLocation layerId) {
        ResourceLocation active = activeOn(player, layerId);
        if (active == null) return 0;
        Origin origin = OriginRegistry.get(active);
        return origin == null ? 0 : origin.impact().level();
    }

    public static int layerCount(Player player) {
        int count = 0;
        for (ResourceLocation held : chosen(player).values()) {
            if (held != null && !held.equals(OriginRegistry.EMPTY_ID)) count++;
        }
        return count;
    }

    public static @Nullable Player playerOf(@Nullable Entity entity) {
        return entity instanceof Player player ? player : null;
    }
}
