package dev.overgrown.origins.item;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

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

        OriginManager.clearAllLayers(player);

        PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
        OriginLayer first = OriginManager.promptLayer(player, state);
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
}
