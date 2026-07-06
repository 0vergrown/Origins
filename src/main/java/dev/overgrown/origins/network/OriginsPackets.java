package dev.overgrown.origins.network;

import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;

public final class OriginsPackets {
    private OriginsPackets() {}

    public static final ResourceLocation SYNC_REGISTRIES = Origins.id("sync_registries");
    public static final ResourceLocation SYNC_BADGES = Origins.id("sync_badges");
    public static final ResourceLocation SYNC_PLAYER_ORIGINS = Origins.id("sync_player_origins");
    public static final ResourceLocation OPEN_CHOOSE_SCREEN = Origins.id("open_choose_screen");
    public static final ResourceLocation CLOSE_CHOOSE_SCREEN = Origins.id("close_choose_screen");
    public static final ResourceLocation CHOOSE_ORIGIN = Origins.id("choose_origin");
}
