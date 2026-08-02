package dev.overgrown.origins;

import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.origins.action.ApplyStoredOriginAction;
import dev.overgrown.origins.action.CopyOriginAction;
import dev.overgrown.origins.action.StoreOriginAction;
import dev.overgrown.origins.action.StoreTargetOriginAction;
import dev.overgrown.origins.action.StoreValueAction;
import dev.overgrown.origins.action.TransferOriginAction;
import dev.overgrown.origins.badge.BadgeLoader;
import dev.overgrown.origins.badge.BadgeManager;
import dev.overgrown.origins.condition.OriginCondition;
import dev.overgrown.origins.condition.StoredOriginCondition;
import dev.overgrown.origins.condition.StoredValueCondition;
import dev.overgrown.origins.storage.StoredDataAttachment;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.event.OriginsServerEvents;
import dev.overgrown.origins.item.OriginsItems;
import dev.overgrown.origins.network.OriginsNetwork;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayerLoader;
import dev.overgrown.origins.origin.OriginLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class Origins implements ModInitializer {
    public static final String MOD_ID = "origins";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        NamespaceAlias.addAlias(MOD_ID, "apoli");

        ConditionTypes.ENTITY.register(id("origin"), new OriginCondition());
        ConditionTypes.ENTITY.register(id("stored_origin"), new StoredOriginCondition());
        ConditionTypes.ENTITY.register(id("stored_value"), new StoredValueCondition());
        ActionTypes.BI_ENTITY.register(id("copy_origin"), new CopyOriginAction());
        ActionTypes.BI_ENTITY.register(id("transfer_origin"), new TransferOriginAction());
        ActionTypes.BI_ENTITY.register(id("store_origin"), new StoreTargetOriginAction());
        ActionTypes.ENTITY.register(id("store_origin"), new StoreOriginAction());
        ActionTypes.ENTITY.register(id("apply_stored_origin"), new ApplyStoredOriginAction());
        ActionTypes.ENTITY.register(id("store_value"), new StoreValueAction());
        PlayerOriginsAttachment.init();
        StoredDataAttachment.init();

        OriginsItems.register();
        OriginsNetwork.registerPayloads();
        OriginsServerNetwork.register();
        OriginsServerEvents.register();
        BadgeManager.init();
        dev.overgrown.origins.origin.OriginPowerSources.register();

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(idWrap(id("origins"), new OriginLoader()));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(idWrap(id("origin_layers"), new OriginLayerLoader()));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(idWrap(id("badges"), new BadgeLoader()));

        LOGGER.info("Origins initialized — '{}' namespace falls back to 'apoli'.", MOD_ID);
    }

    private static net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener idWrap(
        ResourceLocation id, PreparableReloadListener delegate) {
        return new net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                                  net.minecraft.util.profiling.ProfilerFiller prepProfiler,
                                                  net.minecraft.util.profiling.ProfilerFiller applyProfiler,
                                                  Executor prepExecutor, Executor applyExecutor) {
                return delegate.reload(barrier, manager, prepProfiler, applyProfiler, prepExecutor, applyExecutor);
            }
        };
    }
}
