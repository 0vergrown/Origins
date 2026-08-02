package dev.overgrown.origins.event;

import dev.overgrown.origins.command.OriginCommands;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRandomizer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class OriginsServerEvents {
    private OriginsServerEvents() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            OriginCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            OriginsServerNetwork.sendRegistries(player);
            OriginsServerNetwork.sendBadges(player);
            OriginManager.reapplyAll(player);

            OriginRandomizer.onFirstJoin(player);
            OriginManager.checkAutoChoosingLayers(player, true);

            PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);

            OriginsServerNetwork.sendPlayerOriginsTo(player, player);
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other == player) continue;
                OriginsServerNetwork.sendPlayerOriginsTo(player, other);
                OriginsServerNetwork.sendPlayerOriginsTo(other, player);
            }

            dev.overgrown.origins.Origins.LOGGER.info(
                "[Origins] {} joined — persisted origins={}, firstJoinDone={}",
                player.getName().getString(), state.snapshot(), state.firstJoinDone());
            OriginLayer pending = OriginManager.firstUnchosenLayer(player, state);
            if (pending != null) {
                state.setSelectingOrigin(true);
                OriginsServerNetwork.openChooseScreen(player, pending, false);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) OriginRandomizer.onDeath(newPlayer);
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player && ((Player) player).isSleepingLongEnough()) {
                OriginRandomizer.onSleep(player);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PlayerOriginsImpl state = PlayerOriginsAttachment.get(handler.player);
            if (state != null) {
                state.setSelectingOrigin(false);

                dev.overgrown.origins.Origins.LOGGER.info(
                    "[Origins] {} disconnecting — origins to persist={}",
                    handler.player.getName().getString(), state.snapshot());
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                OriginsServerNetwork.sendRegistries(player);
                OriginsServerNetwork.sendBadges(player);
                OriginManager.reapplyAll(player);
                OriginManager.checkAutoChoosingLayers(player, true);

                OriginsServerNetwork.broadcastPlayerOrigins(server, player);
            }
        });
    }
}
