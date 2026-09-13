package dev.overgrown.origins.origin;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum Impact implements StringRepresentable {
    NONE("none", 0),
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3);

    public static final com.mojang.serialization.Codec<Impact> CODEC =
        StringRepresentable.fromEnum(Impact::values);

    public static final com.mojang.serialization.Codec<Integer> LEVEL_CODEC =
        com.mojang.serialization.Codec.either(com.mojang.serialization.Codec.INT, CODEC).xmap(
            either -> either.map(level -> level, Impact::level),
            level -> {
                for (Impact impact : values()) {
                    if (impact.level == level) return com.mojang.datafixers.util.Either.right(impact);
                }
                return com.mojang.datafixers.util.Either.left(level);
            });

    private final String name;
    private final int level;

    Impact(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public int level() {
        return level;
    }

    public Component label() {
        return Component.translatable("origins.gui.impact." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static Impact byName(String name, Impact fallback) {
        for (Impact i : values()) {
            if (i.name.equalsIgnoreCase(name)) return i;
        }
        return fallback;
    }
}
