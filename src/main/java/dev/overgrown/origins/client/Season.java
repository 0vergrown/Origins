package dev.overgrown.origins.client;

import java.time.LocalDate;
import java.time.Month;

public enum Season {
    DEFAULT("default", 0.0F),
    RAINBOW("rainbow", 0.5F),
    FRIGID("frigid", 1.0F);

    private final String dir;
    private final float modelValue;

    Season(String dir, float modelValue) {
        this.dir = dir;
        this.modelValue = modelValue;
    }

    public String dir() {
        return dir;
    }

    public float modelValue() {
        return modelValue;
    }

    public static Season current() {
        switch (dev.overgrown.origins.OriginsConfig.guiTheme()) {
            case "default": return DEFAULT;
            case "rainbow": return RAINBOW;
            case "frigid": return FRIGID;
            default: break;
        }
        LocalDate date = LocalDate.now();
        if (date.getMonth() == Month.JUNE) {
            return RAINBOW;
        }
        if (date.getMonth() == Month.DECEMBER && date.getDayOfMonth() >= 24 && date.getDayOfMonth() <= 26) {
            return FRIGID;
        }
        return DEFAULT;
    }
}
