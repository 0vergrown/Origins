package dev.overgrown.origins.origin;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

public final class OriginRandomizer {

    private static final RandomSource RNG = RandomSource.create();

    private OriginRandomizer() {}

    public static @Nullable ResourceLocation roll(Player player, OriginLayer layer) {
        return roll(player, layer, null);
    }

    public static @Nullable ResourceLocation roll(Player player, OriginLayer layer, @Nullable ResourceLocation avoid) {
        LinkedHashSet<ResourceLocation> eligible = new LinkedHashSet<>();
        for (ResourceLocation id : layer.availableOrigins(player)) {
            if (layer.excludedFromRandom().contains(id)) continue;
            Origin origin = OriginRegistry.get(id);
            if (origin == null) continue;
            if (!origin.choosable() && !layer.randomAllowsUnchoosable()) continue;
            eligible.add(id);
        }
        if (eligible.isEmpty()) return null;

        List<ResourceLocation> list = new ArrayList<>(eligible);
        if (avoid != null && list.size() > 1) {
            list.removeIf(avoid::equals);
        }
        return pick(list, layer.random());
    }

    private static ResourceLocation pick(List<ResourceLocation> list, OriginLayer.RandomConfig config) {
        if (list.size() == 1) return list.get(0);
        boolean weighted = config.style() == OriginLayer.RandomConfig.Style.WEIGHTED
            || (config.style() == OriginLayer.RandomConfig.Style.ROLL && !config.weights().isEmpty());
        if (weighted) {
            int total = 0;
            for (ResourceLocation id : list) total += config.weight(id);
            if (total > 0) {
                int roll = RNG.nextInt(total);
                for (ResourceLocation id : list) {
                    roll -= config.weight(id);
                    if (roll < 0) return id;
                }
            }
        }
        return list.get(RNG.nextInt(list.size()));
    }

    public enum Reason {
        FIRST_JOIN("first_join"), DEATH("death"), SLEEP("sleep"), COMMAND("command");
        private final String key;
        Reason(String key) { this.key = key; }
        public String key() { return key; }
    }

    public static void randomise(ServerPlayer player, OriginLayer layer, Reason reason) {
        OriginLayer.RandomiserConfig cfg = layer.randomiser();
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        ResourceLocation current = state.getOrigin(layer.id());

        ResourceLocation pick;
        boolean rolled = false;
        if (cfg.resetToDefaultOnDeath() && reason == Reason.DEATH && layer.defaultOrigin() != null) {
            pick = layer.defaultOrigin();
        } else {
            pick = roll(player, layer, cfg.allowDuplicate() ? null : current);
            rolled = true;
        }
        if (pick == null) return;

        OriginManager.chooseOrigin(player, layer.id(), pick, false);
        OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        if (rolled) {
            OriginsServerNetwork.sendOriginRoll(player, layer, pick);
        }

        if (cfg.broadcastMessages()) {
            Origin origin = OriginRegistry.get(pick);
            Component name = origin != null ? origin.name() : Component.literal(String.valueOf(pick));
            player.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("origins.randomiser." + reason.key(), player.getDisplayName(), name), false);
        }
    }

    public static void onDeath(ServerPlayer player) {
        List<OriginLayer> layers = randomiserLayers(player, OriginLayer.RandomiserConfig::onDeath);
        if (layers.isEmpty()) return;
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        OriginLayer.RandomiserConfig driver = layers.get(0).randomiser();
        seed(state, driver);

        state.setLivesUntilRandomise(state.livesUntilRandomise() - 1);
        if (driver.livesEnabled()) {
            state.setLives(state.lives() - 1);
            if (state.lives() <= 0) {
                player.setGameMode(GameType.SPECTATOR);
                if (driver.broadcastMessages()) {
                    player.sendSystemMessage(Component.translatable("origins.randomiser.out_of_lives"));
                }
            } else if (driver.broadcastMessages()) {
                player.sendSystemMessage(Component.translatable("origins.randomiser.lives_remaining", state.lives()));
            }
        }

        if (state.livesUntilRandomise() <= 0) {
            state.setLivesUntilRandomise(driver.deathsBetween());
            for (OriginLayer layer : layers) randomise(player, layer, Reason.DEATH);
        } else if (driver.deathsBetween() > 1 && driver.broadcastMessages()) {
            player.sendSystemMessage(Component.translatable("origins.randomiser.deaths_until", state.livesUntilRandomise()));
        }
    }

    public static void onSleep(ServerPlayer player) {
        List<OriginLayer> layers = randomiserLayers(player, OriginLayer.RandomiserConfig::onSleep);
        if (layers.isEmpty()) return;
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        OriginLayer.RandomiserConfig driver = layers.get(0).randomiser();
        seed(state, driver);

        state.setSleepsUntilRandomise(state.sleepsUntilRandomise() - 1);
        if (state.sleepsUntilRandomise() <= 0) {
            state.setSleepsUntilRandomise(driver.sleepsBetween());
            for (OriginLayer layer : layers) randomise(player, layer, Reason.SLEEP);
        } else if (driver.sleepsBetween() > 1 && driver.broadcastMessages()) {
            player.sendSystemMessage(Component.translatable("origins.randomiser.sleeps_until", state.sleepsUntilRandomise()));
        }
    }

    public static boolean onFirstJoin(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        if (state.firstJoinDone()) return false;
        state.setFirstJoinDone(true);

        List<OriginLayer> layers = randomiserLayers(player, OriginLayer.RandomiserConfig::onFirstJoin);
        if (layers.isEmpty()) return false;
        seed(state, layers.get(0).randomiser());
        for (OriginLayer layer : layers) randomise(player, layer, Reason.FIRST_JOIN);
        return true;
    }

    private static List<OriginLayer> randomiserLayers(ServerPlayer player,
                                                      Predicate<OriginLayer.RandomiserConfig> filter) {
        List<OriginLayer> out = new ArrayList<>();
        for (OriginLayer layer : OriginLayers.enabledFor(player)) {
            if (layer.swappable()) continue;
            if (filter.test(layer.randomiser())) out.add(layer);
        }
        return out;
    }

    private static void seed(PlayerOriginsImpl state, OriginLayer.RandomiserConfig driver) {
        if (state.livesUntilRandomise() < 0) state.setLivesUntilRandomise(driver.deathsBetween());
        if (state.sleepsUntilRandomise() < 0) state.setSleepsUntilRandomise(driver.sleepsBetween());
        if (driver.livesEnabled() && state.lives() < 0) state.setLives(driver.startingLives());
    }
}
