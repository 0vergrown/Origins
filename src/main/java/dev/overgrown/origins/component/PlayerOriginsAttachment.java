package dev.overgrown.origins.component;

import dev.overgrown.origins.Origins;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;


public final class PlayerOriginsAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Origins.MOD_ID);

    public static final Supplier<AttachmentType<PlayerOriginsImpl>> TYPE =
        ATTACHMENT_TYPES.register("origins", () -> AttachmentType.builder(PlayerOriginsImpl::new)
            .serialize(PlayerOriginsImpl.CODEC)
            .copyOnDeath()
            .build());

    private PlayerOriginsAttachment() {}

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    public static @Nullable PlayerOriginsImpl get(Player player) {
        return player.hasData(TYPE.get()) ? player.getData(TYPE.get()) : null;
    }

    public static PlayerOriginsImpl getOrCreate(Player player) {
        
        return player.getData(TYPE.get());
    }
}
