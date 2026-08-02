package dev.overgrown.origins.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.data.ItemStackData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Function;

public record OriginIcon(ItemStack stack, Optional<ResourceLocation> texture, int width, int height) {

    public static final OriginIcon EMPTY = new OriginIcon(new ItemStack(Items.AIR), Optional.empty(), 16, 16);

    public OriginIcon {
        stack = stack.copy();
    }

    public static OriginIcon ofItem(ItemStack stack) {
        return new OriginIcon(stack, Optional.empty(), 16, 16);
    }

    public static OriginIcon ofTexture(ResourceLocation texture, int width, int height) {
        return new OriginIcon(new ItemStack(Items.AIR), Optional.of(texture), width, height);
    }

    public boolean isTexture() {
        return texture.isPresent();
    }

    private static final Codec<OriginIcon> TEXTURE_CODEC = RecordCodecBuilder.create(i -> i.group(
        ResourceLocation.CODEC.fieldOf("texture").forGetter(icon -> icon.texture.orElseThrow()),
        Codec.INT.optionalFieldOf("width", 16).forGetter(OriginIcon::width),
        Codec.INT.optionalFieldOf("height", 16).forGetter(OriginIcon::height)
    ).apply(i, OriginIcon::ofTexture));

    private static final Codec<OriginIcon> ITEM_ID_CODEC = ResourceLocation.CODEC.xmap(
        rl -> ofItem(new ItemStack(BuiltInRegistries.ITEM.get(rl))),
        icon -> BuiltInRegistries.ITEM.getKey(icon.stack.getItem()));

    private static final Codec<OriginIcon> ITEM_STACK_CODEC = ItemStackData.CODEC.xmap(
        data -> ofItem(data.stack()),
        icon -> new ItemStackData(icon.stack));

    public static final Codec<OriginIcon> CODEC = Codec.either(
        TEXTURE_CODEC,
        Codec.either(ITEM_ID_CODEC, ITEM_STACK_CODEC)
    ).xmap(
        either -> either.map(Function.identity(), inner -> inner.map(Function.identity(), Function.identity())),
        icon -> icon.isTexture() ? Either.left(icon) : Either.right(Either.right(icon)));

    public void write(FriendlyByteBuf buf) {
        buf.writeItem(stack);
        buf.writeOptional(texture, (b, id) -> b.writeResourceLocation(id));
        buf.writeVarInt(width);
        buf.writeVarInt(height);
    }

    public static OriginIcon read(FriendlyByteBuf buf) {
        ItemStack stack = buf.readItem();
        Optional<ResourceLocation> texture = buf.readOptional(b -> b.readResourceLocation());
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        return new OriginIcon(stack, texture, width, height);
    }
}
