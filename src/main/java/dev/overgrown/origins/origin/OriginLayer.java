package dev.overgrown.origins.origin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.origins.Origins;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
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
    RandomConfig random,
    RandomiserConfig randomiser
) implements Comparable<OriginLayer> {

    public OriginLayer {
        conditionedOrigins = List.copyOf(conditionedOrigins);
        excludedFromRandom = List.copyOf(excludedFromRandom);
    }

    public record RandomConfig(Style style, Map<ResourceLocation, Integer> weights) {
        public enum Style { UNIFORM, WEIGHTED }
        public static final RandomConfig DEFAULT = new RandomConfig(Style.UNIFORM, Map.of());
        public static final MapCodec<RandomConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(
                s -> "weighted".equalsIgnoreCase(s) ? Style.WEIGHTED : Style.UNIFORM,
                style -> style == Style.WEIGHTED ? "weighted" : "uniform"
            ).optionalFieldOf("style", Style.UNIFORM).forGetter(RandomConfig::style),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT.xmap(v -> Math.max(0, v), Function.identity()))
                .optionalFieldOf("weights", Map.of()).forGetter(RandomConfig::weights)
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

    private record RandomBlock(boolean allow, boolean allowUnchoosable, List<ResourceLocation> exclude, RandomConfig config) {
        static final RandomBlock DEFAULT = new RandomBlock(false, false, List.of(), RandomConfig.DEFAULT);
        static final Codec<RandomBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("allow", true).forGetter(RandomBlock::allow),
            Codec.BOOL.optionalFieldOf("allow_unchoosable", false).forGetter(RandomBlock::allowUnchoosable),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(RandomBlock::exclude),
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
            RandomBlock.CODEC.optionalFieldOf("random", RandomBlock.DEFAULT)
                .forGetter(l -> new RandomBlock(l.allowRandom(), l.randomAllowsUnchoosable(), l.excludedFromRandom(), l.random())),
            ResourceLocation.CODEC.optionalFieldOf("default_origin").forGetter(l -> Optional.ofNullable(l.defaultOrigin())),
            Codec.BOOL.optionalFieldOf("auto_choose", false).forGetter(OriginLayer::autoChooseIfNoChoice),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(OriginLayer::hidden),
            RandomiserConfig.CODEC.optionalFieldOf("randomiser", RandomiserConfig.DEFAULT).forGetter(OriginLayer::randomiser)
        ).apply(instance, (origins, order, enabled, name, gui, missingName, missingDesc, random, defaultOrigin, autoChoose, hidden, randomiser) ->
            new OriginLayer(id, order, enabled, origins, name, gui.choose(), gui.view(), missingName, missingDesc,
                random.allow(), random.allowUnchoosable(), random.exclude(), defaultOrigin.orElse(null), autoChoose, hidden,
                random.config(), randomiser)));
    }

    public static OriginLayer fromJson(ResourceLocation id, JsonObject json) {
        return codec(id).codec().parse(JsonOps.INSTANCE, migrate(json))
            .resultOrPartial(err -> Origins.LOGGER.error("Failed to load origin layer {}: {}", id, err))
            .orElseThrow(() -> new JsonParseException("Invalid origin layer: " + id));
    }

    private static JsonObject migrate(JsonObject json) {
        if (json.has("random") && json.get("random").isJsonObject()) {
            JsonObject random = json.getAsJsonObject("random");
            if (!random.has("exclude") && json.has("exclude_random") && json.get("exclude_random").isJsonArray()) {
                JsonObject copy = json.deepCopy();
                copy.getAsJsonObject("random").add("exclude", json.getAsJsonArray("exclude_random"));
                return copy;
            }
            return json;
        }
        JsonObject copy = json.deepCopy();
        JsonObject random = new JsonObject();
        random.addProperty("allow", GsonHelper.getAsBoolean(json, "allow_random", false));
        random.addProperty("allow_unchoosable", GsonHelper.getAsBoolean(json, "allow_random_unchoosable", false));
        if (json.has("exclude_random") && json.get("exclude_random").isJsonArray()) {
            random.add("exclude", json.getAsJsonArray("exclude_random"));
        }
        copy.add("random", random);
        return copy;
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
        return new OriginLayer(id, order, enabled, conditioned, nameKey, chooseTitleKey, viewTitleKey,
            missingNameKey, missingDescriptionKey, allowRandom, randomAllowsUnchoosable,
            excludedFromRandom, defaultOrigin, autoChoose, hidden,
            RandomConfig.DEFAULT, RandomiserConfig.DEFAULT);
    }
}
