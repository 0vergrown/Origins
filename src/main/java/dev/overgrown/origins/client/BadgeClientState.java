package dev.overgrown.origins.client;

import dev.overgrown.origins.badge.Badge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;


@OnlyIn(Dist.CLIENT)
public final class BadgeClientState {

    private static volatile Map<ResourceLocation, List<Badge>> BY_POWER = Map.of();

    private BadgeClientState() {}

    public static void replaceAll(Map<ResourceLocation, List<Badge>> badgesByPower) {
        BY_POWER = Map.copyOf(badgesByPower);
    }

    public static List<Badge> get(ResourceLocation powerId) {
        return BY_POWER.getOrDefault(powerId, List.of());
    }

    public static boolean has(ResourceLocation powerId) {
        return !get(powerId).isEmpty();
    }

    public static void clear() {
        BY_POWER = Map.of();
    }
}
