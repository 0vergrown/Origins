package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.TextComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public record CraftingRecipeBadge(
    ResourceLocation spriteId,
    Optional<ResourceLocation> recipeId,
    NonNullList<ItemStack> inputs,
    ItemStack output,
    int width,
    Optional<Component> prefix,
    Optional<Component> suffix
) implements Badge {

    public static final MapCodec<CraftingRecipeBadge> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ResourceLocation.CODEC.fieldOf("sprite").forGetter(CraftingRecipeBadge::spriteId),
        ResourceLocation.CODEC.fieldOf("recipe").forGetter(b -> b.recipeId().orElse(null)),
        TextComponent.CODEC.optionalFieldOf("prefix").forGetter(CraftingRecipeBadge::prefix),
        TextComponent.CODEC.optionalFieldOf("suffix").forGetter(CraftingRecipeBadge::suffix)
    ).apply(i, (sprite, recipe, prefix, suffix) -> new CraftingRecipeBadge(
        sprite, Optional.of(recipe), NonNullList.create(), ItemStack.EMPTY, 3, prefix, suffix)));

    
    public static CraftingRecipeBadge fromRecipe(ResourceLocation sprite, ResourceLocation recipeId,
                                                 CraftingRecipe recipe, Optional<Component> prefix,
                                                 Optional<Component> suffix, HolderLookup.Provider registries) {
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<Ingredient> ingredients = recipe.getIngredients();
        NonNullList<ItemStack> inputs = NonNullList.withSize(Math.min(9, ingredients.size()), ItemStack.EMPTY);
        for (int idx = 0; idx < inputs.size(); idx++) {
            ItemStack[] stacks = ingredients.get(idx).getItems();
            if (stacks.length > 0) inputs.set(idx, stacks[0]);
        }
        ItemStack output = recipe.getResultItem(registries);
        return new CraftingRecipeBadge(sprite, Optional.of(recipeId), inputs, output, width, prefix, suffix);
    }

    
    public CraftingRecipeBadge resolve(net.minecraft.world.item.crafting.RecipeManager recipeManager,
                                       HolderLookup.Provider registries) {
        if (!output.isEmpty() || recipeId.isEmpty()) return this;
        return recipeManager.byKey(recipeId.get())
            .map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .filter(r -> r instanceof CraftingRecipe)
            .map(r -> fromRecipe(spriteId, recipeId.get(), (CraftingRecipe) r, prefix, suffix, registries))
            .orElse(this);
    }

    @Override
    public ResourceLocation typeId() {
        return BadgeTypes.CRAFTING_RECIPE.id();
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int widthLimit,
                              ResourceLocation powerId, float time) {
        List<Component> lines = new ArrayList<>();
        prefix.ifPresent(lines::add);
        suffix.ifPresent(lines::add);
        Optional<TooltipComponent> visual = output.isEmpty()
            ? Optional.empty()
            : Optional.of(new CraftingRecipeTooltipData(inputs, output, width));
        if (lines.isEmpty() && visual.isEmpty()) return;
        graphics.renderTooltip(font, lines, visual, mouseX, mouseY);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(spriteId);
        buf.writeVarInt(width);
        buf.writeVarInt(inputs.size());
        for (ItemStack stack : inputs) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, output);
        writeOptionalComponent(buf, prefix);
        writeOptionalComponent(buf, suffix);
    }

    public static CraftingRecipeBadge fromNetwork(RegistryFriendlyByteBuf buf) {
        ResourceLocation sprite = buf.readResourceLocation();
        int width = buf.readVarInt();
        int size = buf.readVarInt();
        NonNullList<ItemStack> inputs = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int idx = 0; idx < size; idx++) inputs.set(idx, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        Optional<Component> prefix = readOptionalComponent(buf);
        Optional<Component> suffix = readOptionalComponent(buf);
        return new CraftingRecipeBadge(sprite, Optional.empty(), inputs, output, width, prefix, suffix);
    }

    private static void writeOptionalComponent(RegistryFriendlyByteBuf buf, Optional<Component> component) {
        buf.writeBoolean(component.isPresent());
        component.ifPresent(c -> ComponentSerialization.STREAM_CODEC.encode(buf, c));
    }

    private static Optional<Component> readOptionalComponent(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? Optional.of(ComponentSerialization.STREAM_CODEC.decode(buf)) : Optional.empty();
    }
}
