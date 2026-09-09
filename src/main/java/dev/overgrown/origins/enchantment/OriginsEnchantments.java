package dev.overgrown.origins.enchantment;

import dev.overgrown.origins.Origins;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

public final class OriginsEnchantments {
    public static final Enchantment WATER_PROTECTION = new WaterProtectionEnchantment();

    private OriginsEnchantments() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ENCHANTMENT, Origins.id("water_protection"), WATER_PROTECTION);
    }
}
