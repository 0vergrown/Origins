package dev.overgrown.origins.origin;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum OriginSelection implements StringRepresentable {
    MAIN("main"),
    ACTIVE("active"),
    POOL("pool"),
    ALL("all");

    public static final Codec<OriginSelection> CODEC = StringRepresentable.fromEnum(OriginSelection::values);

    private final String name;

    OriginSelection(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
