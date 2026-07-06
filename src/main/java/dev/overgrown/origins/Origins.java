package dev.overgrown.origins;

import dev.overgrown.apoli.alias.NamespaceAlias;
import dev.overgrown.apoli.action.ActionTypes;
import dev.overgrown.apoli.condition.ConditionTypes;
import dev.overgrown.origins.action.CopyOriginAction;
import dev.overgrown.origins.action.TransferOriginAction;
import dev.overgrown.origins.condition.OriginCondition;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
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

        
        ConditionTypes.ENTITY.register(id("origin"), new OriginCondition());

        
        ActionTypes.BI_ENTITY.register(id("copy_origin"), new CopyOriginAction());
        
        
        ActionTypes.BI_ENTITY.register(id("transfer_origin"), new TransferOriginAction());

        PlayerOriginsAttachment.register(modBus);
        OriginsItems.register(modBus);
        modBus.addListener(OriginsNetwork::register);
        dev.overgrown.origins.badge.BadgeManager.init();

        LOGGER.info("Origins initialized — '{}' namespace falls back to 'apoli'.", MOD_ID);
    }
}
