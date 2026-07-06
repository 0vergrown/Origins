package dev.overgrown.origins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PlayerOriginsImpl {
    private final Map<ResourceLocation, ResourceLocation> originsByLayer;
    private boolean selectingOrigin;
    private int livesUntilRandomise = -1;
    private int lives = -1;
    private int sleepsUntilRandomise = -1;
    private boolean firstJoinDone = false;

    public PlayerOriginsImpl() {
        this.originsByLayer = new HashMap<>();
        this.selectingOrigin = false;
    }

    public PlayerOriginsImpl(Map<ResourceLocation, ResourceLocation> originsByLayer, boolean selectingOrigin,
                             int livesUntilRandomise, int lives, int sleepsUntilRandomise, boolean firstJoinDone) {
        this.originsByLayer = new HashMap<>(originsByLayer);
        this.selectingOrigin = selectingOrigin;
        this.livesUntilRandomise = livesUntilRandomise;
        this.lives = lives;
        this.sleepsUntilRandomise = sleepsUntilRandomise;
        this.firstJoinDone = firstJoinDone;
    }

    public boolean hasOrigin(ResourceLocation layerId) {
        ResourceLocation origin = originsByLayer.get(layerId);
        return origin != null && !origin.equals(OriginRegistry.EMPTY_ID);
    }

    public ResourceLocation getOrigin(ResourceLocation layerId) {
        return originsByLayer.getOrDefault(layerId, OriginRegistry.EMPTY_ID);
    }

    public void setOrigin(ResourceLocation layerId, ResourceLocation originId) {
        originsByLayer.put(layerId, originId);
    }

    public void clearOrigin(ResourceLocation layerId) {
        originsByLayer.remove(layerId);
    }

    public Set<Map.Entry<ResourceLocation, ResourceLocation>> entries() {
        return originsByLayer.entrySet();
    }

    public Map<ResourceLocation, ResourceLocation> snapshot() {
        return Map.copyOf(originsByLayer);
    }

    public boolean isSelectingOrigin() {
        return selectingOrigin;
    }

    public void setSelectingOrigin(boolean selecting) {
        this.selectingOrigin = selecting;
    }

    public int livesUntilRandomise() {
        return livesUntilRandomise;
    }

    public void setLivesUntilRandomise(int value) {
        this.livesUntilRandomise = value;
    }

    public int lives() {
        return lives;
    }

    public void setLives(int value) {
        this.lives = value;
    }

    public int sleepsUntilRandomise() {
        return sleepsUntilRandomise;
    }

    public void setSleepsUntilRandomise(int value) {
        this.sleepsUntilRandomise = value;
    }

    public boolean firstJoinDone() {
        return firstJoinDone;
    }

    public void setFirstJoinDone(boolean value) {
        this.firstJoinDone = value;
    }

    public static final Codec<PlayerOriginsImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)
            .fieldOf("origins")
            .forGetter(PlayerOriginsImpl::snapshot),
        Codec.BOOL.optionalFieldOf("selecting_origin", false)
            .forGetter(PlayerOriginsImpl::isSelectingOrigin),
        Codec.INT.optionalFieldOf("lives_until_randomise", -1)
            .forGetter(PlayerOriginsImpl::livesUntilRandomise),
        Codec.INT.optionalFieldOf("lives", -1)
            .forGetter(PlayerOriginsImpl::lives),
        Codec.INT.optionalFieldOf("sleeps_until_randomise", -1)
            .forGetter(PlayerOriginsImpl::sleepsUntilRandomise),
        Codec.BOOL.optionalFieldOf("first_join_done", false)
            .forGetter(PlayerOriginsImpl::firstJoinDone)
    ).apply(instance, PlayerOriginsImpl::new));
}
