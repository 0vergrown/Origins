package dev.overgrown.origins.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
    float nameScrollSpeed
) {
    public Origin {
        powerEntries = List.copyOf(powerEntries);
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
            Codec.FLOAT.optionalFieldOf("name_scroll_speed", 1.0f).forGetter(Origin::nameScrollSpeed)
        ).apply(instance, (powerEntries, icon, impact, order, loadingPriority, unchoosable, name, description, nameScrollSpeed) ->
            new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, false, name, description, nameScrollSpeed)));
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
            Integer.MAX_VALUE, 0, true, true, Optional.empty(), Optional.empty());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeCollection(powerEntries, (b, e) -> e.write(b));
        icon.write(buf);
        buf.writeEnum(impact);
        buf.writeVarInt(order);
        buf.writeVarInt(loadingPriority);
        buf.writeBoolean(unchoosable);
        buf.writeBoolean(special);
        buf.writeOptional(nameText, FriendlyByteBuf::writeComponent);
        buf.writeOptional(descriptionText, FriendlyByteBuf::writeComponent);
        buf.writeFloat(nameScrollSpeed);
    }

    public static Origin read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        List<OriginPowerEntry> powerEntries = buf.readList(OriginPowerEntry::read);
        IconData icon = IconData.read(buf);
        Impact impact = buf.readEnum(Impact.class);
        int order = buf.readVarInt();
        int loadingPriority = buf.readVarInt();
        boolean unchoosable = buf.readBoolean();
        boolean special = buf.readBoolean();
        Optional<Component> nameText = buf.readOptional(FriendlyByteBuf::readComponent);
        Optional<Component> descriptionText = buf.readOptional(FriendlyByteBuf::readComponent);
        float nameScrollSpeed = buf.readFloat();
        return new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special,
            nameText, descriptionText, nameScrollSpeed);
    }
}
