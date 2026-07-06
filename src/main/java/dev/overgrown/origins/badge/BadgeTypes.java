package dev.overgrown.origins.badge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.overgrown.origins.Origins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class BadgeTypes {

    private static final Map<ResourceLocation, BadgeType<? extends Badge>> TYPES = new LinkedHashMap<>();

    public static final BadgeType<SpriteBadge> SPRITE = register(
        new BadgeType<>(Origins.id("sprite"), SpriteBadge.CODEC, SpriteBadge::fromNetwork));

    public static final BadgeType<TooltipBadge> TOOLTIP = register(
        new BadgeType<>(Origins.id("tooltip"), TooltipBadge.CODEC, TooltipBadge::fromNetwork));

    public static final BadgeType<KeybindBadge> KEYBIND = register(
        new BadgeType<>(Origins.id("keybind"), KeybindBadge.CODEC, KeybindBadge::fromNetwork));

    public static final BadgeType<CraftingRecipeBadge> CRAFTING_RECIPE = register(
        new BadgeType<>(Origins.id("crafting_recipe"), CraftingRecipeBadge.CODEC, CraftingRecipeBadge::fromNetwork));

    public static final ResourceLocation DEFAULT = KEYBIND.id();

    private BadgeTypes() {}

    private static <B extends Badge> BadgeType<B> register(BadgeType<B> type) {
        TYPES.put(type.id(), type);
        return type;
    }

    public static final Codec<ResourceLocation> ID_CODEC = ResourceLocation.CODEC.flatXmap(
        id -> TYPES.containsKey(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Unknown badge type '" + id + "' — badges are Origins-only; "
                + "use one of " + TYPES.keySet() + " (e.g. origins:tooltip)"),
        DataResult::success);

    public static com.mojang.serialization.Codec<? extends Badge> codecFor(ResourceLocation typeId) {
        return TYPES.getOrDefault(typeId, KEYBIND).codec().codec();
    }

    public static Function<FriendlyByteBuf, ? extends Badge> readerFor(ResourceLocation typeId) {
        return TYPES.getOrDefault(typeId, KEYBIND).networkReader();
    }

    public static void touch() {}
}
