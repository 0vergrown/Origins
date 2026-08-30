package dev.overgrown.origins.origin;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

public final class OriginUpgrades {

    private static final int INTERVAL = 20;

    private OriginUpgrades() {}

    public static void tick(MinecraftServer server) {
        if (!OriginRegistry.anyUpgrades()) return;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            if (player.tickCount % INTERVAL != 0) continue;
            check(player);
        }
    }

    private static void check(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || state.isSelectingOrigin()) return;

        for (Map.Entry<ResourceLocation, ResourceLocation> entry : state.snapshot().entrySet()) {
            OriginUpgrade upgrade = matching(player, entry.getValue());
            if (upgrade == null) continue;
            if (OriginManager.upgradeOrigin(player, entry.getKey(), upgrade.origin())) announce(player, upgrade);
        }

        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : state.poolSnapshot().entrySet()) {
            List<ResourceLocation> pool = entry.getValue();
            for (int i = 0; i < pool.size(); i++) {
                OriginUpgrade upgrade = matching(player, pool.get(i));
                if (upgrade == null) continue;
                if (OriginManager.upgradePooledOrigin(player, entry.getKey(), pool.get(i), upgrade.origin())) {
                    announce(player, upgrade);
                }
            }
        }
    }

    private static OriginUpgrade matching(ServerPlayer player, ResourceLocation originId) {
        Origin origin = OriginRegistry.get(originId);
        if (origin == null || origin.upgrades().isEmpty()) return null;
        List<OriginUpgrade> upgrades = origin.upgrades();
        for (int i = 0; i < upgrades.size(); i++) {
            OriginUpgrade upgrade = upgrades.get(i);
            if (upgrade.origin().equals(originId)) continue;
            if (OriginRegistry.get(upgrade.origin()) == null) continue;
            if (upgrade.test(player)) return upgrade;
        }
        return null;
    }

    private static void announce(ServerPlayer player, OriginUpgrade upgrade) {
        upgrade.announcement().ifPresent(message -> player.sendSystemMessage(message));
    }
}
