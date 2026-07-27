package dev.overgrown.origins.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.overgrown.apoli.command.ApoliPermissions;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.storage.OriginStorage;
import dev.overgrown.origins.storage.StoredData;
import dev.overgrown.origins.storage.StoredDataAttachment;
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

import java.util.List;

public final class StorageCommands {
    private StorageCommands() {}

    private static final SuggestionProvider<CommandSourceStack> LAYERS = (ctx, b) ->
        SharedSuggestionProvider.suggestResource(OriginLayers.all().stream().map(OriginLayer::id), b);

    private static final SuggestionProvider<CommandSourceStack> KEYS = (ctx, b) -> {
        try {
            StoredData data = StoredDataAttachment.get(EntityArgument.getPlayer(ctx, "target"));
            return SharedSuggestionProvider.suggest(data == null ? List.of() : data.keys(), b);
        } catch (CommandSyntaxException | IllegalArgumentException e) {
            return SharedSuggestionProvider.suggest(List.of(), b);
        }
    };

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("storage")
            .requires(ApoliPermissions.require("origins.command.origin.storage", 2))
            .then(Commands.literal("list")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(StorageCommands::list)))
            .then(Commands.literal("get")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("key", StringArgumentType.word()).suggests(KEYS)
                        .executes(StorageCommands::get))))
            .then(Commands.literal("store")
                .then(Commands.literal("origin")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("key", StringArgumentType.word())
                            .then(Commands.argument("source", EntityArgument.player())
                                .executes(ctx -> storeOrigin(ctx, OriginStorage.DEFAULT_LAYER))
                                .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                                    .executes(ctx -> storeOrigin(ctx, ResourceLocationArgument.getId(ctx, "layer"))))))))
                .then(Commands.literal("value")
                    .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("key", StringArgumentType.word())
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(StorageCommands::storeValue))))))
            .then(Commands.literal("apply")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("key", StringArgumentType.word()).suggests(KEYS)
                        .executes(ctx -> apply(ctx, null))
                        .then(Commands.argument("layer", ResourceLocationArgument.id()).suggests(LAYERS)
                            .executes(ctx -> apply(ctx, ResourceLocationArgument.getId(ctx, "layer")))))))
            .then(Commands.literal("clear")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(ctx -> clear(ctx, null))
                    .then(Commands.argument("key", StringArgumentType.word()).suggests(KEYS)
                        .executes(ctx -> clear(ctx, StringArgumentType.getString(ctx, "key"))))))
            .then(Commands.literal("run")
                .then(Commands.argument("target", EntityArgument.player())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(StorageCommands::run))));
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        StoredData data = StoredDataAttachment.get(target);
        if (data == null || data.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " has no stored data."), false);
            return 0;
        }
        MutableComponent out = Component.literal("Stored data for " + target.getGameProfile().getName() + ":")
            .withStyle(ChatFormatting.BOLD);
        for (var entry : data.originsView().entrySet()) {
            Origin origin = OriginRegistry.get(entry.getValue().origin());
            out.append(Component.literal("\n  " + entry.getKey() + " = ").withStyle(ChatFormatting.WHITE))
                .append(origin != null ? origin.name() : Component.literal(entry.getValue().origin().toString()))
                .append(Component.literal(" (" + entry.getValue().layer() + ")").withStyle(ChatFormatting.GRAY));
        }
        for (var entry : data.valuesView().entrySet()) {
            out.append(Component.literal("\n  " + entry.getKey() + " = " + entry.getValue()).withStyle(ChatFormatting.WHITE));
        }
        ctx.getSource().sendSuccess(() -> out, false);
        return data.keys().size();
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String key = StringArgumentType.getString(ctx, "key");
        String value = OriginStorage.lookup(target, key);
        if (value == null) {
            ctx.getSource().sendFailure(Component.literal("No stored value under '" + key + "'"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(key + " = " + value), false);
        return 1;
    }

    private static int storeOrigin(CommandContext<CommandSourceStack> ctx, ResourceLocation layerId) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ServerPlayer source = EntityArgument.getPlayer(ctx, "source");
        String key = StringArgumentType.getString(ctx, "key");
        if (!OriginStorage.storeOrigin(target, key, source, layerId)) {
            ctx.getSource().sendFailure(Component.literal(
                source.getGameProfile().getName() + " has no origin on " + layerId));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Stored " + source.getGameProfile().getName()
            + "'s " + layerId + " origin as '" + key + "' on " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int storeValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String key = StringArgumentType.getString(ctx, "key");
        String raw = StringArgumentType.getString(ctx, "value");
        String value = OriginStorage.resolve(target, raw);
        if (value == null) {
            ctx.getSource().sendFailure(Component.literal("Unresolved placeholder in: " + raw));
            return 0;
        }
        OriginStorage.storeValue(target, key, value);
        ctx.getSource().sendSuccess(() -> Component.literal("Stored '" + key + "' = " + value
            + " on " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, ResourceLocation layerId) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String key = StringArgumentType.getString(ctx, "key");
        if (!OriginStorage.applyStoredOrigin(target, key, layerId, false)) {
            ctx.getSource().sendFailure(Component.literal("No usable origin stored under '" + key + "'"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Applied stored origin '" + key
            + "' to " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, String key) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        if (!OriginStorage.clear(target, key)) {
            ctx.getSource().sendFailure(Component.literal("Nothing to clear."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + (key == null ? "all stored data" : "'" + key + "'")
            + " on " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String template = StringArgumentType.getString(ctx, "command");
        String command = OriginStorage.resolve(target, template);
        if (command == null) return 0;
        MinecraftServer server = target.getServer();
        if (server == null) return 0;
        CommandSourceStack source = server.createCommandSourceStack()
            .withSuppressedOutput()
            .withPosition(target.position())
            .withRotation(target.getRotationVector())
            .withEntity(target);
        server.getCommands().performPrefixedCommand(source, command);
        return 1;
    }
}
