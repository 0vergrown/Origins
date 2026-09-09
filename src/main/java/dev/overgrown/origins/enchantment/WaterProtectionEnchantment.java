package dev.overgrown.origins.enchantment;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;

public final class WaterProtectionEnchantment extends Enchantment {

    public static final TagKey<DamageType> PROTECTED_FROM = TagKey.create(
        Registries.DAMAGE_TYPE, new ResourceLocation("origins", "water_protection"));

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
    public int getDamageProtection(int level, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return 0;
        return source.is(PROTECTED_FROM) ? level * 2 : 0;
    }

    @Override
    protected boolean checkCompatibility(Enchantment other) {
        if (other instanceof ProtectionEnchantment protection) {
            return protection.type == ProtectionEnchantment.Type.FALL;
        }
        return super.checkCompatibility(other);
    }
}
