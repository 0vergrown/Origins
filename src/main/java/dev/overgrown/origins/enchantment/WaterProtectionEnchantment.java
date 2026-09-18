package dev.overgrown.origins.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;

public final class WaterProtectionEnchantment extends Enchantment {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public WaterProtectionEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR, ARMOR_SLOTS);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 8;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        if (other instanceof ProtectionEnchantment protection) {
            return protection.type == ProtectionEnchantment.Type.FALL;
        }
        return super.checkCompatibility(other);
    }
}
