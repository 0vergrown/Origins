package dev.overgrown.origins.storage;

import dev.overgrown.origins.Origins;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class StoredDataAttachment {
    public static final AttachmentType<StoredData> TYPE = AttachmentRegistry.<StoredData>builder()
        .initializer(StoredData::new)
        .persistent(StoredData.CODEC)
        .copyOnDeath()
        .buildAndRegister(Origins.id("stored_data"));

    private StoredDataAttachment() {}

    public static void init() {}

    public static @Nullable StoredData get(Player player) {
        return player.getAttached(TYPE);
    }

    public static StoredData getOrCreate(Player player) {
        return player.getAttachedOrCreate(TYPE);
    }
}
