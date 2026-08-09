package dev.overgrown.origins.event;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayerLoader;
import dev.overgrown.origins.origin.OriginLoader;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRandomizer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

@EventBusSubscriber(modid = Origins.MOD_ID)
public final class OriginsServerEvents {
    private OriginsServerEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        dev.overgrown.origins.command.OriginCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new OriginLoader());
        event.addListener(new OriginLayerLoader());
        event.addListener(new dev.overgrown.origins.badge.BadgeLoader());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        OriginsServerNetwork.sendRegistries(player);
        OriginsServerNetwork.sendBadges(player);
        OriginManager.reapplyAll(player);

        OriginRandomizer.onFirstJoin(player);
        OriginManager.checkAutoChoosingLayers(player, true);

        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);

        OriginsServerNetwork.sendPlayerOriginsTo(player, player);
        OriginsServerNetwork.sendPlayerSwapsTo(player, player);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player) continue;
            OriginsServerNetwork.sendPlayerOriginsTo(player, other);
            OriginsServerNetwork.sendPlayerOriginsTo(other, player);
            OriginsServerNetwork.sendPlayerSwapsTo(player, other);
            OriginsServerNetwork.sendPlayerSwapsTo(other, player);
        }
        OriginLayer pending = OriginManager.firstUnchosenLayer(player, state);
        if (pending != null) {
            state.setSelectingOrigin(true);
            OriginsServerNetwork.openChooseScreen(player, pending, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) {
            OriginRandomizer.onDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isSleepingLongEnough()) {
            OriginRandomizer.onSleep(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state != null) state.setSelectingOrigin(false);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {

        if (event.getPlayer() != null) return;
        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            OriginsServerNetwork.sendRegistries(player);
            OriginsServerNetwork.sendBadges(player);
            OriginManager.reapplyAll(player);
            OriginManager.checkAutoChoosingLayers(player, true);

            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        }
    }
}
