package dev.overgrown.origins.badge;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record CraftingRecipeTooltipData(NonNullList<ItemStack> inputs, ItemStack output, int width)
    implements TooltipComponent {
}
