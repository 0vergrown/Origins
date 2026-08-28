package dev.overgrown.origins.origin;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.origins.Origins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record OriginLayer(
    ResourceLocation id,
    int order,
    boolean enabled,
    List<ConditionedOrigin> conditionedOrigins,
    String nameKey,
    String chooseTitleKey,
    String viewTitleKey,
    String missingNameKey,
    String missingDescriptionKey,
    boolean allowRandom,
    boolean randomAllowsUnchoosable,
    List<ResourceLocation> excludedFromRandom,
    @Nullable ResourceLocation defaultOrigin,
    boolean autoChooseIfNoChoice,
    boolean hidden,
    boolean revalidate,
    RandomConfig random,
    RandomiserConfig randomiser,
    SwapConfig swap,
    int maxPlayersPerOrigin
) implements Comparable<OriginLayer> {

    public OriginLayer {
        conditionedOrigins = List.copyOf(conditionedOrigins);
        excludedFromRandom = List.copyOf(excludedFromRandom);
    }

    public record RandomConfig(Style style, Map<ResourceLocation, Integer> weights, int rollDuration) {
        public enum Style { UNIFORM, WEIGHTED, ROLL }
        public static final RandomConfig DEFAULT = new RandomConfig(Style.UNIFORM, Map.of(), 80);

        public static final MapCodec<RandomConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(
                s -> "weighted".equalsIgnoreCase(s) ? Style.WEIGHTED
                    : ("roll".equalsIgnoreCase(s) || "gacha".equalsIgnoreCase(s)) ? Style.ROLL
                    : Style.UNIFORM,
                style -> style == Style.WEIGHTED ? "weighted" : style == Style.ROLL ? "roll" : "uniform"
            ).optionalFieldOf("style", Style.UNIFORM).forGetter(RandomConfig::style),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.xmap(v -> Math.max(0, v), Function.identity()))
                .optionalFieldOf("weights", Map.of()).forGetter(RandomConfig::weights),
            Codec.INT.optionalFieldOf("roll_duration", 80).forGetter(RandomConfig::rollDuration)
        ).apply(instance, RandomConfig::new));

        public RandomConfig {
            weights = Map.copyOf(weights);
        }

        public int weight(ResourceLocation id) {
            return Math.max(0, weights.getOrDefault(id, 1));
        }
    }

    public record RandomiserConfig(boolean onFirstJoin, boolean onDeath, boolean onSleep,
                                   int deathsBetween, int sleepsBetween,
                                   boolean livesEnabled, int startingLives,
                                   boolean resetToDefaultOnDeath, boolean showScreenOnDeath,
                                   boolean allowDuplicate, boolean broadcastMessages) {
        public static final RandomiserConfig DEFAULT =
            new RandomiserConfig(false, false, false, 1, 1, false, 10, false, false, false, true);

        public static final Codec<RandomiserConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("on_first_join", false).forGetter(RandomiserConfig::onFirstJoin),
            Codec.BOOL.optionalFieldOf("on_death", false).forGetter(RandomiserConfig::onDeath),
            Codec.BOOL.optionalFieldOf("on_sleep", false).forGetter(RandomiserConfig::onSleep),
            Codec.INT.xmap(v -> Math.max(1, v), Function.identity())
                .optionalFieldOf("deaths_between_randomises", 1).forGetter(RandomiserConfig::deathsBetween),
            Codec.INT.xmap(v -> Math.max(1, v), Function.identity())
                .optionalFieldOf("sleeps_between_randomises", 1).forGetter(RandomiserConfig::sleepsBetween),
            Lives.CODEC.optionalFieldOf("lives", Lives.DEFAULT)
                .forGetter(rc -> new Lives(rc.livesEnabled(), rc.startingLives())),
            Codec.BOOL.optionalFieldOf("reset_to_default_on_death", false).forGetter(RandomiserConfig::resetToDefaultOnDeath),
            Codec.BOOL.optionalFieldOf("show_screen_on_death", false).forGetter(RandomiserConfig::showScreenOnDeath),
            Codec.BOOL.optionalFieldOf("allow_duplicate", false).forGetter(RandomiserConfig::allowDuplicate),
            Codec.BOOL.optionalFieldOf("broadcast_messages", true).forGetter(RandomiserConfig::broadcastMessages)
        ).apply(instance, (onFirstJoin, onDeath, onSleep, deaths, sleeps, lives, reset, showScreen, allowDup, broadcast) ->
            new RandomiserConfig(onFirstJoin, onDeath, onSleep, deaths, sleeps, lives.enabled(), lives.starting(),
                reset, showScreen, allowDup, broadcast)));

        public boolean active() {
            return onFirstJoin || onDeath || onSleep;
        }
    }

    private record Lives(boolean enabled, int starting) {
        static final Lives DEFAULT = new Lives(false, 10);
        static final Codec<Lives> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Lives::enabled),
            Codec.INT.xmap(v -> Math.max(1, v), Function.identity()).optionalFieldOf("starting", 10).forGetter(Lives::starting)
        ).apply(instance, Lives::new));
    }

    private record GuiTitle(String choose, String view) {
        static final GuiTitle DEFAULT = new GuiTitle("", "");
        static final Codec<GuiTitle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("choose_origin", "").forGetter(GuiTitle::choose),
            Codec.STRING.optionalFieldOf("view_origin", "").forGetter(GuiTitle::view)
        ).apply(instance, GuiTitle::new));
    }

    public record SwapConfig(boolean enabled, Optional<ResourceLocation> targetLayer,
                             boolean shiftReturnsToMain, boolean wrapToMain) {
        public static final SwapConfig DISABLED = new SwapConfig(false, Optional.empty(), true, true);
        public static final SwapConfig ENABLED = new SwapConfig(true, Optional.empty(), true, true);

        private static final Codec<SwapConfig> OBJECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(SwapConfig::enabled),
            ResourceLocation.CODEC.optionalFieldOf("target_layer").forGetter(SwapConfig::targetLayer),
            Codec.BOOL.optionalFieldOf("shift_returns_to_main", true).forGetter(SwapConfig::shiftReturnsToMain),
            Codec.BOOL.optionalFieldOf("wrap_to_main", true).forGetter(SwapConfig::wrapToMain)
        ).apply(instance, SwapConfig::new));

        static final Codec<SwapConfig> CODEC = Codec.either(Codec.BOOL, OBJECT_CODEC).xmap(
            either -> either.map(flag -> flag ? ENABLED : DISABLED, Function.identity()),
            config -> config.isPlain()
                ? com.mojang.datafixers.util.Either.left(config.enabled())
                : com.mojang.datafixers.util.Either.right(config));

        private boolean isPlain() {
            return targetLayer.isEmpty() && shiftReturnsToMain && wrapToMain;
        }
    }

    private record LegacyRandom(boolean allow, boolean allowUnchoosable, List<ResourceLocation> exclude) {
        static final LegacyRandom NONE = new LegacyRandom(false, false, List.of());
        static final MapCodec<LegacyRandom> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("allow_random", false).forGetter(LegacyRandom::allow),
            Codec.BOOL.optionalFieldOf("allow_random_unchoosable", false).forGetter(LegacyRandom::allowUnchoosable),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclude_random", List.of()).forGetter(LegacyRandom::exclude)
        ).apply(instance, LegacyRandom::new));
    }

    private record RandomBlock(boolean allow, boolean allowUnchoosable, Optional<List<ResourceLocation>> exclude, RandomConfig config) {
        static final Codec<RandomBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("allow", true).forGetter(RandomBlock::allow),
            Codec.BOOL.optionalFieldOf("allow_unchoosable", false).forGetter(RandomBlock::allowUnchoosable),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclude").forGetter(RandomBlock::exclude),
            RandomConfig.CODEC.forGetter(RandomBlock::config)
        ).apply(instance, RandomBlock::new));
    }

    public Component name() {
        String key = nameKey.isEmpty() ? "layer." + id.getNamespace() + "." + id.getPath() + ".name" : nameKey;
        return Component.translatable(key);
    }

    public Component chooseTitle() {
        String key = chooseTitleKey.isEmpty() ? "layer." + id.getNamespace() + "." + id.getPath() + ".choose_origin.name" : chooseTitleKey;
        return Component.translatable(key);
    }

    public Component viewTitle() {
        String key = viewTitleKey.isEmpty() ? "layer." + id.getNamespace() + "." + id.getPath() + ".view_origin.name" : viewTitleKey;
        return Component.translatable(key);
    }

    public boolean swappable() {
        return swap.enabled();
    }

    public List<ResourceLocation> availableOrigins(Player player) {
        List<ResourceLocation> out = new ArrayList<>();
        for (ConditionedOrigin co : conditionedOrigins) {
            if (co.test(player)) out.addAll(co.origins());
        }
        return out;
    }

    public List<ResourceLocation> allOrigins() {
        List<ResourceLocation> out = new ArrayList<>();
        for (ConditionedOrigin co : conditionedOrigins) out.addAll(co.origins());
        return out;
    }

    @Override
    public int compareTo(OriginLayer o) {
        return Integer.compare(order, o.order);
    }

    public static MapCodec<OriginLayer> codec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            ConditionedOrigin.CODEC.listOf().fieldOf("origins").forGetter(OriginLayer::conditionedOrigins),
            Codec.INT.optionalFieldOf("order", 0).forGetter(OriginLayer::order),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(OriginLayer::enabled),
            Codec.STRING.optionalFieldOf("name", "").forGetter(OriginLayer::nameKey),
            GuiTitle.CODEC.optionalFieldOf("gui_title", GuiTitle.DEFAULT)
                .forGetter(l -> new GuiTitle(l.chooseTitleKey(), l.viewTitleKey())),
            Codec.STRING.optionalFieldOf("missing_name", "").forGetter(OriginLayer::missingNameKey),
            Codec.STRING.optionalFieldOf("missing_description", "").forGetter(OriginLayer::missingDescriptionKey),
            RandomBlock.CODEC.optionalFieldOf("random")
                .forGetter(l -> Optional.of(new RandomBlock(l.allowRandom(), l.randomAllowsUnchoosable(),
                    Optional.of(l.excludedFromRandom()), l.random()))),
            LegacyRandom.MAP_CODEC.forGetter(l -> LegacyRandom.NONE),
            ResourceLocation.CODEC.optionalFieldOf("default_origin").forGetter(l -> Optional.ofNullable(l.defaultOrigin())),
            Codec.BOOL.optionalFieldOf("auto_choose", false).forGetter(OriginLayer::autoChooseIfNoChoice),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(OriginLayer::hidden),
            Codec.BOOL.optionalFieldOf("revalidate").forGetter(l -> Optional.of(l.revalidate())),
            RandomiserConfig.CODEC.optionalFieldOf("randomiser", RandomiserConfig.DEFAULT).forGetter(OriginLayer::randomiser),
            SwapConfig.CODEC.optionalFieldOf("swappable", SwapConfig.DISABLED).forGetter(OriginLayer::swap),
            Codec.INT.optionalFieldOf("max_players_per_origin", OriginCaps.UNLIMITED)
                .forGetter(OriginLayer::maxPlayersPerOrigin)
        ).apply(instance, (origins, order, enabled, name, gui, missingName, missingDesc, randomBlock,
                           legacy, defaultOrigin, autoChoose, hidden,
                           revalidate, randomiser, swap, maxPlayersPerOrigin) -> {
            boolean allowRandom;
            boolean allowUnchoosable;
            List<ResourceLocation> exclude;
            RandomConfig randomConfig;
            if (randomBlock.isPresent()) {
                RandomBlock rb = randomBlock.get();
                allowRandom = rb.allow();
                allowUnchoosable = rb.allowUnchoosable();
                exclude = rb.exclude().orElse(legacy.exclude());
                randomConfig = rb.config();
            } else {
                allowRandom = legacy.allow();
                allowUnchoosable = legacy.allowUnchoosable();
                exclude = legacy.exclude();
                randomConfig = RandomConfig.DEFAULT;
            }
            return new OriginLayer(id, order, enabled, origins, name, gui.choose(), gui.view(), missingName, missingDesc,
                allowRandom, allowUnchoosable, exclude, defaultOrigin.orElse(null), autoChoose, hidden,
                revalidate.orElse(autoChoose), randomConfig, randomiser, swap, maxPlayersPerOrigin);
        }));
    }

    public static OriginLayer fromJson(ResourceLocation id, JsonElement json) {
        return codec(id).codec().parse(JsonOps.INSTANCE, json)
            .resultOrPartial(err -> Origins.LOGGER.error("Failed to load origin layer {}: {}", id, err))
            .orElseThrow(() -> new IllegalArgumentException("Invalid origin layer: " + id));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeVarInt(order);
        buf.writeBoolean(enabled);
        buf.writeCollection(conditionedOrigins, (b, co) -> co.write(b));
        buf.writeUtf(nameKey);
        buf.writeUtf(chooseTitleKey);
        buf.writeUtf(viewTitleKey);
        buf.writeUtf(missingNameKey);
        buf.writeUtf(missingDescriptionKey);
        buf.writeBoolean(allowRandom);
        buf.writeBoolean(randomAllowsUnchoosable);
        buf.writeCollection(excludedFromRandom, FriendlyByteBuf::writeResourceLocation);
        buf.writeOptional(Optional.ofNullable(defaultOrigin), FriendlyByteBuf::writeResourceLocation);
        buf.writeBoolean(autoChooseIfNoChoice);
        buf.writeBoolean(hidden);
        buf.writeBoolean(revalidate);
        buf.writeEnum(random.style());
        buf.writeVarInt(random.rollDuration());
        buf.writeMap(random.weights(), FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::writeVarInt);
        buf.writeBoolean(randomiser.onFirstJoin());
        buf.writeBoolean(randomiser.onDeath());
        buf.writeBoolean(randomiser.onSleep());
        buf.writeVarInt(randomiser.deathsBetween());
        buf.writeVarInt(randomiser.sleepsBetween());
        buf.writeBoolean(randomiser.livesEnabled());
        buf.writeVarInt(randomiser.startingLives());
        buf.writeBoolean(randomiser.resetToDefaultOnDeath());
        buf.writeBoolean(randomiser.showScreenOnDeath());
        buf.writeBoolean(randomiser.allowDuplicate());
        buf.writeBoolean(randomiser.broadcastMessages());
        buf.writeBoolean(swap.enabled());
        buf.writeOptional(swap.targetLayer(), FriendlyByteBuf::writeResourceLocation);
        buf.writeBoolean(swap.shiftReturnsToMain());
        buf.writeBoolean(swap.wrapToMain());
        buf.writeVarInt(maxPlayersPerOrigin);
    }

    public static OriginLayer read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        int order = buf.readVarInt();
        boolean enabled = buf.readBoolean();
        List<ConditionedOrigin> conditioned = buf.readList(ConditionedOrigin::read);
        String nameKey = buf.readUtf();
        String chooseTitleKey = buf.readUtf();
        String viewTitleKey = buf.readUtf();
        String missingNameKey = buf.readUtf();
        String missingDescriptionKey = buf.readUtf();
        boolean allowRandom = buf.readBoolean();
        boolean randomAllowsUnchoosable = buf.readBoolean();
        List<ResourceLocation> excludedFromRandom = buf.readList(FriendlyByteBuf::readResourceLocation);
        ResourceLocation defaultOrigin = buf.readOptional(FriendlyByteBuf::readResourceLocation).orElse(null);
        boolean autoChoose = buf.readBoolean();
        boolean hidden = buf.readBoolean();
        boolean revalidate = buf.readBoolean();
        RandomConfig.Style style = buf.readEnum(RandomConfig.Style.class);
        int rollDuration = buf.readVarInt();
        Map<ResourceLocation, Integer> weights = buf.readMap(FriendlyByteBuf::readResourceLocation, FriendlyByteBuf::readVarInt);
        RandomConfig randomConfig = new RandomConfig(style, weights, rollDuration);
        RandomiserConfig randomiserConfig = new RandomiserConfig(
            buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
            buf.readVarInt(), buf.readVarInt(),
            buf.readBoolean(), buf.readVarInt(),
            buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
        SwapConfig swapConfig = new SwapConfig(buf.readBoolean(),
            buf.readOptional(FriendlyByteBuf::readResourceLocation),
            buf.readBoolean(), buf.readBoolean());

        int maxPlayersPerOrigin = buf.readVarInt();

        return new OriginLayer(id, order, enabled, conditioned, nameKey, chooseTitleKey, viewTitleKey,
            missingNameKey, missingDescriptionKey, allowRandom, randomAllowsUnchoosable,
            excludedFromRandom, defaultOrigin, autoChoose, hidden, revalidate,
            randomConfig, randomiserConfig, swapConfig, maxPlayersPerOrigin);
    }
}
