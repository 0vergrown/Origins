package dev.overgrown.origins.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record Origin(
    ResourceLocation id,
    List<OriginPowerEntry> powerEntries,
    IconData icon,
    Impact impact,
    int order,
    int loadingPriority,
    boolean unchoosable,
    boolean special,
    Optional<Component> nameText,
    Optional<Component> descriptionText,
    float nameScrollSpeed,
    int maxPlayers,
    List<OriginUpgrade> upgrades,
    List<String> tags
) {
    public Origin {
        powerEntries = List.copyOf(powerEntries);
        upgrades = List.copyOf(upgrades);
        tags = List.copyOf(tags);
    }

    public Origin(ResourceLocation id, List<OriginPowerEntry> powerEntries, IconData icon, Impact impact,
                  int order, int loadingPriority, boolean unchoosable, boolean special,
                  Optional<Component> nameText, Optional<Component> descriptionText,
                  float nameScrollSpeed, int maxPlayers, List<OriginUpgrade> upgrades) {
        this(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special, nameText, descriptionText,
            nameScrollSpeed, maxPlayers, upgrades, List.of());
    }

    public boolean hasTag(String tag) {
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equals(tag)) return true;
        }
        return false;
    }

    public Origin(ResourceLocation id, List<OriginPowerEntry> powerEntries, IconData icon, Impact impact,
                  int order, int loadingPriority, boolean unchoosable, boolean special,
                  Optional<Component> nameText, Optional<Component> descriptionText, float nameScrollSpeed) {
        this(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special, nameText, descriptionText,
            nameScrollSpeed, OriginCaps.INHERIT, List.of());
    }

    public Origin(ResourceLocation id, List<OriginPowerEntry> powerEntries, IconData icon, Impact impact,
                  int order, int loadingPriority, boolean unchoosable, boolean special,
                  Optional<Component> nameText, Optional<Component> descriptionText) {
        this(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special, nameText, descriptionText, 1.0f);
    }

    public List<ResourceLocation> powers() {
        List<ResourceLocation> out = new ArrayList<>();
        for (OriginPowerEntry entry : powerEntries) out.addAll(entry.powers());
        return out;
    }

    public List<ResourceLocation> powersFor(Player player) {
        List<ResourceLocation> out = new ArrayList<>();
        for (OriginPowerEntry entry : powerEntries) {
            if (entry.visible(player)) out.addAll(entry.powers());
        }
        return out;
    }

    private static final Codec<Impact> IMPACT_CODEC = Codec.either(
        Codec.STRING.xmap(s -> Impact.byName(s, Impact.NONE), Impact::getSerializedName),
        Codec.INT.xmap(Origin::impactByLevel, Impact::level)
    ).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        impact -> Either.<Impact, Impact>left(impact)
    );

    private static final Codec<List<String>> TAGS_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
        either -> either.map(List::of, List::copyOf),
        list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list));

    private static Impact impactByLevel(int level) {
        for (Impact impact : Impact.values()) {
            if (impact.level() == level) return impact;
        }
        return Impact.NONE;
    }

    public static MapCodec<Origin> codec(ResourceLocation id) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
            OriginPowerEntry.CODEC.listOf().optionalFieldOf("powers", List.of()).forGetter(Origin::powerEntries),
            IconData.CODEC.optionalFieldOf("icon", IconData.EMPTY).forGetter(Origin::icon),
            IMPACT_CODEC.optionalFieldOf("impact", Impact.NONE).forGetter(Origin::impact),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Origin::order),
            Codec.INT.optionalFieldOf("loading_priority", 0).forGetter(Origin::loadingPriority),
            Codec.BOOL.optionalFieldOf("unchoosable", false).forGetter(Origin::unchoosable),
            TextComponent.CODEC.optionalFieldOf("name").forGetter(Origin::nameText),
            TextComponent.CODEC.optionalFieldOf("description").forGetter(Origin::descriptionText),
            Codec.FLOAT.optionalFieldOf("name_scroll_speed", 1.0f).forGetter(Origin::nameScrollSpeed),
            Codec.INT.optionalFieldOf("max_players", OriginCaps.INHERIT).forGetter(Origin::maxPlayers),
            OriginUpgrade.CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(Origin::upgrades),
            TAGS_CODEC.optionalFieldOf("tags", List.of()).forGetter(Origin::tags)
        ).apply(instance, (powerEntries, icon, impact, order, loadingPriority, unchoosable, name, description,
                           nameScrollSpeed, maxPlayers, upgrades, tags) ->
            new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, false, name, description,
                nameScrollSpeed, maxPlayers, upgrades, tags)));
    }

    public Component name() {
        return nameText.orElseGet(() -> Component.translatable("origin." + id.getNamespace() + "." + id.getPath() + ".name"));
    }

    public Component description() {
        return descriptionText.orElseGet(() -> Component.translatable("origin." + id.getNamespace() + "." + id.getPath() + ".description"));
    }

    public boolean choosable() {
        return !unchoosable;
    }

    public static Origin empty(ResourceLocation id) {
        return new Origin(id, Collections.emptyList(), IconData.EMPTY, Impact.NONE,
            Integer.MAX_VALUE, 0, true, true, Optional.empty(), Optional.empty(), 1.0f, OriginCaps.UNLIMITED,
            List.of(), List.of());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeCollection(powerEntries, (b, e) -> e.write(b));
        icon.write(buf);
        buf.writeEnum(impact);
        buf.writeVarInt(order);
        buf.writeVarInt(loadingPriority);
        buf.writeBoolean(unchoosable);
        buf.writeBoolean(special);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, nameText);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, descriptionText);
        buf.writeFloat(nameScrollSpeed);
        buf.writeVarInt(maxPlayers);
        buf.writeVarInt(upgrades.size());
        for (OriginUpgrade upgrade : upgrades) upgrade.write(buf);
        buf.writeCollection(tags, FriendlyByteBuf::writeUtf);
    }

    public static Origin read(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        List<OriginPowerEntry> powerEntries = buf.readList(OriginPowerEntry::read);
        IconData icon = IconData.read(buf);
        Impact impact = buf.readEnum(Impact.class);
        int order = buf.readVarInt();
        int loadingPriority = buf.readVarInt();
        boolean unchoosable = buf.readBoolean();
        boolean special = buf.readBoolean();
        Optional<Component> nameText = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
        Optional<Component> descriptionText = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
        float nameScrollSpeed = buf.readFloat();
        int maxPlayers = buf.readVarInt();
        int upgradeCount = buf.readVarInt();
        List<OriginUpgrade> upgrades = new ArrayList<>(upgradeCount);
        for (int i = 0; i < upgradeCount; i++) {
            OriginUpgrade upgrade = OriginUpgrade.read(buf);
            if (upgrade != null) upgrades.add(upgrade);
        }
        List<String> tags = buf.readList(FriendlyByteBuf::readUtf);
        return new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special,
            nameText, descriptionText, nameScrollSpeed, maxPlayers, upgrades, tags);
    }
}
