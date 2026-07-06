package dev.overgrown.origins.badge;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;


public record BadgeType<B extends Badge>(
    ResourceLocation id,
    MapCodec<B> codec,
    Function<RegistryFriendlyByteBuf, B> networkReader
) {}
