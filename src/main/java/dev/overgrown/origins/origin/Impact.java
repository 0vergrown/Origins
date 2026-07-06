package dev.overgrown.origins.origin;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;


public enum Impact implements StringRepresentable {
    NONE("none", 0),
    LOW("low", 1),
    MEDIUM("medium", 2),
    HIGH("high", 3);

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
