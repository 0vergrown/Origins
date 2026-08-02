package dev.overgrown.origins.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.Badge;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class BadgeClientState {

    private static final int DEFAULT_SPRITE_SIZE = 9;

    private static volatile Map<ResourceLocation, List<Badge>> BY_POWER = Map.of();
    private static final Map<ResourceLocation, Integer> SPRITE_SIZES = new HashMap<>();

    private BadgeClientState() {}

    public static void replaceAll(Map<ResourceLocation, List<Badge>> badgesByPower) {
        BY_POWER = Map.copyOf(badgesByPower);
        SPRITE_SIZES.clear();
    }

    public static int spriteSize(ResourceLocation sprite) {
        Integer cached = SPRITE_SIZES.get(sprite);
        if (cached != null) return cached;
        int size = DEFAULT_SPRITE_SIZE;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(sprite);
             NativeImage image = NativeImage.read(in)) {
            size = Math.max(1, image.getWidth());
        } catch (Exception e) {
            Origins.LOGGER.warn("[Origins] Could not read badge sprite {}, assuming {}x{}: {}",
                sprite, DEFAULT_SPRITE_SIZE, DEFAULT_SPRITE_SIZE, e.toString());
        }
        SPRITE_SIZES.put(sprite, size);
        return size;
    }

    public static List<Badge> get(ResourceLocation powerId) {
        return BY_POWER.getOrDefault(powerId, List.of());
    }

    public static boolean has(ResourceLocation powerId) {
        return !get(powerId).isEmpty();
    }

    public static void clear() {
        BY_POWER = Map.of();
        SPRITE_SIZES.clear();
    }
}
