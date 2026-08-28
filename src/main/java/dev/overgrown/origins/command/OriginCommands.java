package dev.overgrown.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.overgrown.apoli.command.ApoliPermissions;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginCaps;
import dev.overgrown.origins.origin.OriginClaims;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRandomizer;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.SwapManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class OriginCommands {

    private OriginCommands() {}

    private static final SuggestionProvider<CommandSourceStack> LAYERS = (ctx, b) ->
        SharedSuggestionProvider.suggestResource(OriginLayers.all().stream().map(OriginLayer::id), b);
    private static final SuggestionProvider<CommandSourceStack> ORIGINS = (ctx, b) -> {
        OriginLayer layer = layerArg(ctx);
        java.util.stream.Stream<ResourceLocation> ids = layer != null
            ? java.util.stream.Stream.concat(layer.allOrigins().stream(), java.util.stream.Stream.of(OriginRegistry.EMPTY_ID))
            : OriginRegistry.all().stream().map(Origin::id);
        return SharedSuggestionProvider.suggestResource(ids, b);
    };

    private static OriginLayer layerArg(CommandContext<CommandSourceStack> ctx) {
        try {
            return OriginLayers.get(ResourceLocationArgument.getId(ctx, "layer"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("origin")
            .then(Commands.literal("set").requires(ApoliPermissions.require("origins.command.origin.set", 2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .then(Commands.argument("origin", ResourceLocationArgument.id()).suggests(ORIGINS)
                            .executes(OriginCommands::set)))))
            .then(Commands.literal("has")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .then(Commands.argument("origin", ResourceLocationArgument.id()).suggests(ORIGINS)
                            .executes(OriginCommands::has)))))
            .then(Commands.literal("get")
                .executes(OriginCommands::getAll)
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(OriginCommands::getOne))))
            .then(Commands.literal("gui").requires(ApoliPermissions.require("origins.command.origin.gui", 2))
                .executes(ctx -> gui(ctx, List.of(ctx.getSource().getPlayerOrException()), null))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> gui(ctx, EntityArgument.getPlayers(ctx, "targets"), null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> gui(ctx, EntityArgument.getPlayers(ctx, "targets"),
                            ResourceLocationArgument.getId(ctx, "layer"))))))
            .then(Commands.literal("random").requires(ApoliPermissions.require("origins.command.origin.random", 2))
                .executes(ctx -> random(ctx, List.of(ctx.getSource().getPlayerOrException()), null))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> random(ctx, EntityArgument.getPlayers(ctx, "targets"), null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> random(ctx, EntityArgument.getPlayers(ctx, "targets"),
                            ResourceLocationArgument.getId(ctx, "layer"))))))
            .then(Commands.literal("revoke").requires(ApoliPermissions.require("origins.command.origin.set", 2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> revoke(ctx, null))
                        .then(Commands.argument("origin", ResourceLocationArgument.id()).suggests(ORIGINS)
                            .executes(ctx -> revoke(ctx, ResourceLocationArgument.getId(ctx, "origin")))))))
            .then(Commands.literal("cap")
                .requires(ApoliPermissions.require("origins.command.origin.cap", 2))
                .executes(ctx -> capList(ctx, null))
                .then(Commands.literal("list")
                    .executes(ctx -> capList(ctx, null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> capList(ctx, ResourceLocationArgument.getId(ctx, "layer")))))
                .then(Commands.literal("clear")
                    .executes(ctx -> capClear(ctx, null, null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> capClear(ctx, ResourceLocationArgument.getId(ctx, "layer"), null))
                        .then(Commands.argument("origin", ResourceLocationArgument.id()).suggests(ORIGINS)
                            .executes(ctx -> capClear(ctx, ResourceLocationArgument.getId(ctx, "layer"),
                                ResourceLocationArgument.getId(ctx, "origin")))))))
            .then(StorageCommands.build()));
    }

    private static int capList(CommandContext<CommandSourceStack> ctx, ResourceLocation onlyLayer) {
        MinecraftServer server = ctx.getSource().getServer();
        OriginClaims claims = OriginClaims.get(server);
        MutableComponent out = Component.literal("Origin caps:").withStyle(ChatFormatting.BOLD);
        int listed = 0;
        for (OriginLayer layer : OriginLayers.all()) {
            if (onlyLayer != null && !onlyLayer.equals(layer.id())) continue;
            for (ResourceLocation originId : layer.allOrigins()) {
                int limit = OriginCaps.limitOf(layer.id(), originId);
                if (limit <= OriginCaps.UNLIMITED) continue;
                java.util.Set<java.util.UUID> holders = claims.holders(layer.id(), originId);
                listed++;
                out.append(Component.literal("\n - " + layer.id() + " / " + originId + ": ")
                        .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(holders.size() + "/" + limit)
                        .withStyle(holders.size() >= limit ? ChatFormatting.RED : ChatFormatting.GREEN))
                    .append(Component.literal(holders.isEmpty() ? "" : " " + names(server, holders))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (listed == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal(onlyLayer == null
                ? "No origin declares a player cap."
                : "No origin on layer " + onlyLayer + " declares a player cap."), false);
            return 0;
        }
        int total = listed;
        ctx.getSource().sendSuccess(() -> out, false);
        return total;
    }

    private static String names(MinecraftServer server, java.util.Set<java.util.UUID> holders) {
        StringBuilder builder = new StringBuilder("[");
        for (java.util.UUID uuid : holders) {
            if (builder.length() > 1) builder.append(", ");
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online != null) {
                builder.append(online.getGameProfile().getName());
                continue;
            }
            builder.append(server.getProfileCache() == null
                ? uuid.toString()
                : server.getProfileCache().get(uuid).map(com.mojang.authlib.GameProfile::getName).orElse(uuid.toString()));
        }
        return builder.append(']').toString();
    }

    private static int capClear(CommandContext<CommandSourceStack> ctx, ResourceLocation layerId,
                                ResourceLocation originId) {
        MinecraftServer server = ctx.getSource().getServer();
        OriginClaims claims = OriginClaims.get(server);
        boolean cleared;
        String what;
        if (layerId == null) {
            cleared = claims.clearAll();
            what = "every layer";
        } else if (originId == null) {
            cleared = claims.clearLayer(layerId);
            what = layerId.toString();
        } else {
            cleared = claims.clearOrigin(layerId, originId);
            what = layerId + " / " + originId;
        }
        if (!cleared) {
            ctx.getSource().sendFailure(Component.literal("No origin claims recorded for " + what + "."));
            return 0;
        }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) OriginManager.refreshClaims(online);
        OriginsServerNetwork.broadcastOriginCaps(server);
        String target = what;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared origin claims for " + target
            + ". Players who are online kept theirs; offline holders released their slot."), true);
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx, ResourceLocation originId)
        throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation layerId = ResourceLocationArgument.getId(ctx, "layer");
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown origin layer: " + layerId));
            return 0;
        }
        int affected = 0;
        for (ServerPlayer player : targets) {
            boolean changed;
            if (originId == null) {
                OriginManager.removeOrigin(player, layerId);
                changed = true;
                for (ResourceLocation granted : List.copyOf(SwapManager.grantedPoolOf(player, layerId))) {
                    SwapManager.revokeFromAnyPool(player, layerId, granted);
                }
            } else {
                changed = SwapManager.revokeFromAnyPool(player, layerId, originId);
                PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
                if (!changed && state != null && originId.equals(state.getOrigin(layerId))) {
                    OriginManager.removeOrigin(player, layerId);
                    changed = true;
                }
            }
            if (!changed) continue;
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
            affected++;
        }
        int changed = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked "
            + (originId == null ? "everything on " + layerId : originId + " from " + layerId)
            + " for " + changed + " player(s)"), true);
        return changed;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation layerId = ResourceLocationArgument.getId(ctx, "layer");
        ResourceLocation originId = ResourceLocationArgument.getId(ctx, "origin");
        OriginLayer layer = OriginLayers.get(layerId);
        if (layer == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown origin layer: " + layerId));
            return 0;
        }
        Origin origin = OriginRegistry.get(originId);
        if (origin == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown origin: " + originId));
            return 0;
        }
        if (!origin.special() && !layer.swappable() && !layer.allOrigins().contains(originId)) {
            ctx.getSource().sendFailure(Component.literal("Origin " + originId + " is not part of layer " + layerId));
            return 0;
        }
        boolean clearing = OriginRegistry.EMPTY_ID.equals(originId);
        int applied = 0;
        for (ServerPlayer player : targets) {
            if (clearing) {
                OriginManager.removeOrigin(player, layerId);
                OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
                applied++;
                continue;
            }
            OriginManager.chooseOrigin(player, layerId, originId, false);
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
            PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
            if (layer.swappable()) {
                if (state != null && state.poolOf(layerId).contains(originId)) {
                    applied++;
                } else {
                    ctx.getSource().sendFailure(Component.literal(originId
                        + " could not be added to the swap pool of " + layerId));
                }
                continue;
            }
            ResourceLocation now = state == null ? OriginRegistry.EMPTY_ID : state.getOrigin(layerId);
            if (originId.equals(now)) {
                applied++;
            } else {
                ctx.getSource().sendFailure(Component.literal(player.getGameProfile().getName()
                    + " is on " + now + " for " + layerId + " — that layer's conditions decide it."));
            }
        }
        int changed = applied;
        if (clearing) {
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + layerId
                + " for " + changed + " player(s); its conditions decide it again"), true);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal("Set ").append(origin.name())
                .append(" for " + changed + " player(s) on " + layerId), true);
        }
        return changed;
    }

    private static int has(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        ResourceLocation layerId = ResourceLocationArgument.getId(ctx, "layer");
        ResourceLocation originId = ResourceLocationArgument.getId(ctx, "origin");
        int count = 0;
        for (ServerPlayer player : targets) {
            PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
            if (state != null && originId.equals(state.getOrigin(layerId))) count++;
        }
        int matched = count;
        ctx.getSource().sendSuccess(() -> Component.literal(matched + "/" + targets.size()
            + " player(s) have " + originId + " on " + layerId), false);
        return count;
    }

    private static int getAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No players online."), false);
            return 0;
        }
        MutableComponent list = Component.literal("Origins:").withStyle(ChatFormatting.BOLD);
        for (ServerPlayer player : players) {
            list.append(Component.literal(" - " + player.getGameProfile().getName() + ": ").withStyle(ChatFormatting.WHITE))
                .append(originsOf(player));
        }
        source.sendSuccess(() -> list, false);
        return players.size();
    }

    private static int getOne(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ResourceLocation layerId = ResourceLocationArgument.getId(ctx, "layer");
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(target);
        ResourceLocation originId = state == null ? OriginRegistry.EMPTY_ID : state.getOrigin(layerId);
        Origin origin = OriginRegistry.get(originId);
        Component name = origin != null ? origin.name() : Component.literal(String.valueOf(originId));
        ctx.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " — " + layerId + ": ")
            .append(name), false);
        return 1;
    }

    private static int gui(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                           ResourceLocation layerId) {
        int opened = 0;
        for (ServerPlayer player : targets) {
            OriginLayer layer = layerId != null
                ? OriginLayers.get(layerId)
                : OriginManager.firstUnchosenLayer(player, PlayerOriginsAttachment.getOrCreate(player));
            if (layer == null && layerId == null) {
                List<OriginLayer> enabled = OriginLayers.enabledFor(player);
                if (enabled.isEmpty()) continue;
                layer = enabled.get(0);
            }
            if (layer == null) continue;
            PlayerOriginsAttachment.getOrCreate(player).setSelectingOrigin(true);
            OriginsServerNetwork.openChooseScreen(player, layer, false);
            opened++;
        }
        int finalOpened = opened;
        ctx.getSource().sendSuccess(() -> Component.literal("Opened the origin screen for " + finalOpened + " player(s)"), true);
        return opened;
    }

    private static int random(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets,
                              ResourceLocation layerId) {
        int rolled = 0;
        for (ServerPlayer player : targets) {
            List<OriginLayer> layers = layerId != null
                ? (OriginLayers.get(layerId) == null ? List.of() : List.of(OriginLayers.get(layerId)))
                : OriginLayers.enabledFor(player);
            for (OriginLayer layer : layers) {
                if (layer.swappable()) continue;
                ResourceLocation pick = OriginRandomizer.roll(player, layer);
                if (pick != null) {
                    OriginManager.chooseOrigin(player, layer.id(), pick, false);
                    OriginsServerNetwork.sendOriginRoll(player, layer, pick);
                    rolled++;
                }
            }
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        }
        int finalRolled = rolled;
        ctx.getSource().sendSuccess(() -> Component.literal("Randomised " + finalRolled + " origin(s)"), true);
        return rolled;
    }

    private static Component originsOf(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        if (state == null || state.snapshot().isEmpty()) {
            return Component.literal("—").withStyle(ChatFormatting.GRAY);
        }
        MutableComponent out = Component.empty();
        boolean first = true;
        for (var entry : state.snapshot().entrySet()) {
            Origin origin = OriginRegistry.get(entry.getValue());
            if (origin == null || entry.getValue().equals(OriginRegistry.EMPTY_ID)) continue;
            if (!first) out.append(", ");
            out.append(origin.name());
            first = false;
        }
        return first ? Component.literal("—").withStyle(ChatFormatting.GRAY) : out;
    }
}
