package dev.overgrown.origins.network;

import dev.overgrown.origins.Origins;
import net.minecraft.resources.ResourceLocation;

public final class OriginsPackets {
    private OriginsPackets() {}

    public static final ResourceLocation SYNC_REGISTRIES = Origins.id("sync_registries");
    public static final ResourceLocation SYNC_BADGES = Origins.id("sync_badges");
    public static final ResourceLocation SYNC_PLAYER_ORIGINS = Origins.id("sync_player_origins");
    public static final ResourceLocation OPEN_CHOOSE_SCREEN = Origins.id("open_choose_screen");
    public static final ResourceLocation ORIGIN_ROLL = Origins.id("origin_roll");
    public static final ResourceLocation CLOSE_CHOOSE_SCREEN = Origins.id("close_choose_screen");
    public static final ResourceLocation CHOOSE_ORIGIN = Origins.id("choose_origin");
    public static final ResourceLocation SYNC_PLAYER_SWAPS = Origins.id("sync_player_swaps");
    public static final ResourceLocation OPEN_SWAP_SCREEN = Origins.id("open_swap_screen");
    public static final ResourceLocation SWAP_CYCLE = Origins.id("swap_cycle");
    public static final ResourceLocation SWAP_SELECT = Origins.id("swap_select");
    public static final ResourceLocation SWAP_MAIN = Origins.id("main");
}
