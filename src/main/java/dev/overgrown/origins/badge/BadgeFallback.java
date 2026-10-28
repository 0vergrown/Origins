package dev.overgrown.origins.badge;

import com.mojang.serialization.Codec;
import dev.overgrown.apoli.codec.SingleOrList;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class BadgeFallback {

    public static final Codec<List<ResourceLocation>> CODEC = SingleOrList.of(ResourceLocation.CODEC);

    private BadgeFallback() {}
}
