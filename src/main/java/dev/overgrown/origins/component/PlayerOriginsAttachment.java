package dev.overgrown.origins.component;

import dev.overgrown.origins.Origins;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class PlayerOriginsAttachment {
    public static final AttachmentType<PlayerOriginsImpl> TYPE = AttachmentRegistry.<PlayerOriginsImpl>builder()
        .initializer(PlayerOriginsImpl::new)
        .persistent(PlayerOriginsImpl.CODEC)
        .copyOnDeath()
        .buildAndRegister(Origins.id("origins"));

    private PlayerOriginsAttachment() {}

    public static void init() {}

    public static @Nullable PlayerOriginsImpl get(Player player) {
        return player.getAttached(TYPE);
    }

    public static PlayerOriginsImpl getOrCreate(Player player) {
        return player.getAttachedOrCreate(TYPE);
    }
}
