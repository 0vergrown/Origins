package dev.overgrown.origins.badge;

import com.mojang.serialization.JsonOps;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.ActionOnKeyPressPower;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.apoli.power.builtin.RecipePower;
import dev.overgrown.apoli.power.builtin.TogglePower;
import dev.overgrown.origins.Origins;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class BadgeManager {

    public static final Map<ResourceLocation, Badge> STANDALONE = new LinkedHashMap<>();
    public static final Map<ResourceLocation, List<Badge>> BY_POWER = new LinkedHashMap<>();

    private static final ResourceLocation TOGGLE_SPRITE = Origins.id("textures/gui/badge/toggle.png");
    private static final ResourceLocation ACTIVE_SPRITE = Origins.id("textures/gui/badge/active.png");
    private static final ResourceLocation RECIPE_SPRITE = Origins.id("textures/gui/badge/recipe.png");

    private BadgeManager() {}

    public static void init() {
        BadgeTypes.touch();
    }

    public static void clear() {
        STANDALONE.clear();
        BY_POWER.clear();
    }

    public static Map<ResourceLocation, List<Badge>> collectForSend(MinecraftServer server) {
        RecipeManager recipes = server.getRecipeManager();
        HolderLookup.Provider registries = server.registryAccess();

        Map<ResourceLocation, List<Badge>> out = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Power> entry : ApoliPowers.view().entrySet()) {
            ResourceLocation id = entry.getKey();
            List<Badge> badges = badgesFor(id, entry.getValue(), recipes, registries);
            if (!badges.isEmpty()) out.put(id, badges);
        }
        return out;
    }

    private static List<Badge> badgesFor(ResourceLocation id, Power power, RecipeManager recipes,
                                         HolderLookup.Provider registries) {
        List<Badge> explicit = BY_POWER.get(id);
        if (explicit != null && !explicit.isEmpty()) {
            return resolve(explicit, recipes, registries);
        }

        if (power.type() instanceof MultiplePower && power.config() instanceof MultiplePower.Cfg cfg) {
            List<Badge> merged = new LinkedList<>();
            for (ResourceLocation subId : cfg.subPowerIds()) {
                Power sub = ApoliPowers.get(subId);
                if (sub != null) merged.addAll(badgesFor(subId, sub, recipes, registries));
            }
            return merged;
        }

        return autoBadges(id, power, recipes, registries);
    }

    private static List<Badge> resolve(List<Badge> badges, RecipeManager recipes, HolderLookup.Provider registries) {
        List<Badge> out = new LinkedList<>();
        for (Badge badge : badges) {
            out.add(badge instanceof CraftingRecipeBadge crafting ? crafting.resolve(recipes, registries) : badge);
        }
        return out;
    }

    private static List<Badge> autoBadges(ResourceLocation id, Power power, RecipeManager recipes,
                                          HolderLookup.Provider registries) {
        Object cfg = power.config();
        if (power.type() instanceof TogglePower && cfg instanceof TogglePower.Config toggle) {
            return List.of(new KeybindBadge(TOGGLE_SPRITE, "origins.gui.badge.toggle", toggle.key().key()));
        }
        if (power.type() instanceof ActionOnKeyPressPower && cfg instanceof ActionOnKeyPressPower.Config active) {
            return List.of(new KeybindBadge(ACTIVE_SPRITE, "origins.gui.badge.active", active.key().key()));
        }
        if (power.type() instanceof RecipePower && cfg instanceof RecipePower.Config recipeCfg) {
            CraftingRecipeBadge badge = recipeAutoBadge(recipeCfg, registries);
            if (badge != null) return List.of(badge);
        }
        return List.of();
    }

    private static CraftingRecipeBadge recipeAutoBadge(RecipePower.Config cfg, HolderLookup.Provider registries) {
        if (cfg.recipeId() == null) return null;
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Recipe<?> recipe = Recipe.CODEC.parse(ops, cfg.recipe())
            .resultOrPartial(err -> Origins.LOGGER.warn("Bad recipe for auto badge {}: {}", cfg.recipeId(), err))
            .orElse(null);
        if (!(recipe instanceof CraftingRecipe crafting)) return null;
        String type = crafting instanceof ShapedRecipe ? "shaped" : crafting instanceof ShapelessRecipe ? "shapeless" : "unknown";
        Component prefix = Component.translatable("origins.gui.badge.recipe.crafting." + type);
        return CraftingRecipeBadge.fromRecipe(RECIPE_SPRITE, cfg.recipeId(), crafting,
            java.util.Optional.of(prefix), java.util.Optional.empty(), registries);
    }
}
