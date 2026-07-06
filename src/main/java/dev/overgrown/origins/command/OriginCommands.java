package dev.overgrown.origins.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRandomizer;
import dev.overgrown.origins.origin.OriginRegistry;
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
            .then(Commands.literal("set").requires(s -> s.hasPermission(2))
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
            .then(Commands.literal("gui").requires(s -> s.hasPermission(2))
                .executes(ctx -> gui(ctx, List.of(ctx.getSource().getPlayerOrException()), null))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> gui(ctx, EntityArgument.getPlayers(ctx, "targets"), null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> gui(ctx, EntityArgument.getPlayers(ctx, "targets"),
                            ResourceLocationArgument.getId(ctx, "layer"))))))
            .then(Commands.literal("random").requires(s -> s.hasPermission(2))
                .executes(ctx -> random(ctx, List.of(ctx.getSource().getPlayerOrException()), null))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> random(ctx, EntityArgument.getPlayers(ctx, "targets"), null))
                    .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                        .executes(ctx -> random(ctx, EntityArgument.getPlayers(ctx, "targets"),
                            ResourceLocationArgument.getId(ctx, "layer")))))));
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
        if (!origin.special() && !layer.allOrigins().contains(originId)) {
            ctx.getSource().sendFailure(Component.literal("Origin " + originId + " is not part of layer " + layerId));
            return 0;
        }
        for (ServerPlayer player : targets) {
            OriginManager.chooseOrigin(player, layerId, originId, false);
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Set ").append(origin.name())
            .append(" for " + targets.size() + " player(s) on " + layerId), true);
        return targets.size();
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
                ResourceLocation pick = OriginRandomizer.roll(player, layer);
                if (pick != null) {
                    OriginManager.chooseOrigin(player, layer.id(), pick, false);
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
