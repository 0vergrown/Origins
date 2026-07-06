package dev.overgrown.origins.item;

import dev.overgrown.apoli.PowerContainerAttachment;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.Map;


public final class OrbOfOriginItem extends Item {

    public OrbOfOriginItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide || !(user instanceof ServerPlayer player)) {
            return InteractionResultHolder.consume(stack);
        }

        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);

        
        PowerContainer container = PowerContainerAttachment.getOrCreate(player);
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : state.snapshot().entrySet()) {
            if (container != null) {
                container.removeAllFromSource(layerSource(entry.getKey()));
            }
            state.clearOrigin(entry.getKey());
        }

        
        OriginLayer first = OriginManager.firstUnchosenLayer(player, state);
        if (first != null) {
            state.setSelectingOrigin(true);
            OriginsServerNetwork.openChooseScreen(player, first, true);
        }
        OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);

        
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    
    private static ResourceLocation layerSource(ResourceLocation layerId) {
        return ResourceLocation.fromNamespaceAndPath(layerId.getNamespace(), "layer/" + layerId.getPath());
    }
}
