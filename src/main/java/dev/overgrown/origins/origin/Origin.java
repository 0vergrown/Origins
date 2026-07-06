package dev.overgrown.origins.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.ItemStackData;
import dev.overgrown.apoli.data.TextComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public record Origin(
    ResourceLocation id,
    List<OriginPowerEntry> powerEntries,
    ItemStack icon,
    Impact impact,
    int order,
    int loadingPriority,
    boolean unchoosable,
    boolean special,
    Optional<Component> nameText,
    Optional<Component> descriptionText
) {
    public Origin {
        powerEntries = List.copyOf(powerEntries);
        icon = icon.copy();
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

    
    private static final Codec<ItemStack> ICON_CODEC = Codec.either(
        ResourceLocation.CODEC.xmap(
            rl -> new ItemStack(BuiltInRegistries.ITEM.get(rl)),
            stack -> BuiltInRegistries.ITEM.getKey(stack.getItem())),
        ItemStackData.CODEC.xmap(ItemStackData::stack, ItemStackData::new)
    ).xmap(
        either -> either.map(Function.identity(), Function.identity()),
        stack -> Either.<ItemStack, ItemStack>left(stack)
    );

    
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
            ICON_CODEC.optionalFieldOf("icon", new ItemStack(Items.AIR)).forGetter(Origin::icon),
            IMPACT_CODEC.optionalFieldOf("impact", Impact.NONE).forGetter(Origin::impact),
            Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(Origin::order),
            Codec.INT.optionalFieldOf("loading_priority", 0).forGetter(Origin::loadingPriority),
            Codec.BOOL.optionalFieldOf("unchoosable", false).forGetter(Origin::unchoosable),
            TextComponent.CODEC.optionalFieldOf("name").forGetter(Origin::nameText),
            TextComponent.CODEC.optionalFieldOf("description").forGetter(Origin::descriptionText)
        ).apply(instance, (powerEntries, icon, impact, order, loadingPriority, unchoosable, name, description) ->
            new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, false, name, description)));
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
        return new Origin(id, Collections.emptyList(), new ItemStack(Items.AIR), Impact.NONE,
            Integer.MAX_VALUE, 0, true, true, Optional.empty(), Optional.empty());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeCollection(powerEntries, (b, e) -> e.write(b));
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, icon);
        buf.writeEnum(impact);
        buf.writeVarInt(order);
        buf.writeVarInt(loadingPriority);
        buf.writeBoolean(unchoosable);
        buf.writeBoolean(special);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, nameText);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buf, descriptionText);
    }

    public static Origin read(RegistryFriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        List<OriginPowerEntry> powerEntries = buf.readList(OriginPowerEntry::read);
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        Impact impact = buf.readEnum(Impact.class);
        int order = buf.readVarInt();
        int loadingPriority = buf.readVarInt();
        boolean unchoosable = buf.readBoolean();
        boolean special = buf.readBoolean();
        Optional<Component> nameText = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
        Optional<Component> descriptionText = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buf);
        return new Origin(id, powerEntries, icon, impact, order, loadingPriority, unchoosable, special,
            nameText, descriptionText);
    }
}
