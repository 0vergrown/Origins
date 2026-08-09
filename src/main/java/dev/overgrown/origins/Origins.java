package dev.overgrown.origins;

import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.origins.action.ApplyStoredOriginAction;
import dev.overgrown.origins.action.CopyOriginAction;
import dev.overgrown.origins.action.StoreOriginAction;
import dev.overgrown.origins.action.StoreTargetOriginAction;
import dev.overgrown.origins.action.StoreValueAction;
import dev.overgrown.origins.action.TransferOriginAction;
import dev.overgrown.origins.condition.OriginCondition;
import dev.overgrown.origins.condition.StoredOriginCondition;
import dev.overgrown.origins.condition.StoredValueCondition;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.storage.StoredDataAttachment;
import dev.overgrown.origins.item.OriginsItems;
import dev.overgrown.origins.network.OriginsNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Origins.MOD_ID)
public final class Origins {
    public static final String MOD_ID = "origins";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public Origins(IEventBus modBus, ModContainer container) {
        NamespaceAlias.addAlias(MOD_ID, "apoli");

        OriginsConfig.register(container);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            dev.overgrown.origins.client.config.OriginsConfigScreens.register(container);
        }

        ConditionTypes.ENTITY.register(id("origin"), new OriginCondition());
        ConditionTypes.ENTITY.register(id("stored_origin"), new StoredOriginCondition());
        ConditionTypes.ENTITY.register(id("stored_value"), new StoredValueCondition());

        ActionTypes.BI_ENTITY.register(id("copy_origin"), new CopyOriginAction());

        ActionTypes.BI_ENTITY.register(id("transfer_origin"), new TransferOriginAction());

        ActionTypes.BI_ENTITY.register(id("store_origin"), new StoreTargetOriginAction());
        ActionTypes.ENTITY.register(id("store_origin"), new StoreOriginAction());
        ActionTypes.ENTITY.register(id("apply_stored_origin"), new ApplyStoredOriginAction());
        ActionTypes.ENTITY.register(id("store_value"), new StoreValueAction());
        ConditionTypes.ENTITY.register(id("swapped"), new dev.overgrown.origins.condition.SwappedCondition());
        ActionTypes.ENTITY.register(id("force_swap"), new dev.overgrown.origins.action.ForceSwapAction());
        ActionTypes.ENTITY.register(id("open_swap_menu"), new dev.overgrown.origins.action.OpenSwapMenuAction());
        dev.overgrown.apoli.power.PowerTypeRegistry.register(
            id("action_on_swap"), new dev.overgrown.origins.power.ActionOnSwapPower());

        PlayerOriginsAttachment.register(modBus);
        StoredDataAttachment.register(modBus);
        OriginsItems.register(modBus);
        modBus.addListener(OriginsNetwork::register);
        dev.overgrown.origins.badge.BadgeManager.init();
        dev.overgrown.origins.origin.OriginPowerSources.register();

        LOGGER.info("Origins initialized — '{}' namespace falls back to 'apoli'.", MOD_ID);
    }
}
