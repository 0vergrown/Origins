package dev.overgrown.origins.item;

import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class OrbOfOriginItem extends Item {

    public static final String LAYER_KEY = "OriginLayer";
    public static final String ORIGIN_KEY = "Origin";

    public OrbOfOriginItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    public static ItemStack forLayer(ResourceLocation layerId, @Nullable ResourceLocation originId) {
        ItemStack stack = new ItemStack(OriginsItems.ORB_OF_ORIGIN);
        CompoundTag tag = new CompoundTag();
        tag.putString(LAYER_KEY, layerId.toString());
        if (originId != null) tag.putString(ORIGIN_KEY, originId.toString());
        writeData(stack, tag);
        return stack;
    }

    private static @Nullable ResourceLocation read(ItemStack stack, String key) {
        CompoundTag tag = readData(stack);
        if (tag == null || !tag.contains(key)) return null;
        return ResourceLocation.tryParse(tag.getString(key));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (world.isClientSide || !(user instanceof ServerPlayer player)) {
            return InteractionResultHolder.consume(stack);
        }

        ResourceLocation layerId = read(stack, LAYER_KEY);
        if (layerId == null) {
            OriginManager.clearAllLayers(player);
            PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
            OriginLayer first = OriginManager.promptLayer(player, state);
            if (first != null) {
                state.setSelectingOrigin(true);
                OriginsServerNetwork.openChooseScreen(player, first, true);
            }
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
            consume(player, stack);
            return InteractionResultHolder.success(stack);
        }

        OriginLayer layer = OriginLayers.get(layerId);
        if (layer == null || layer.swappable()) return InteractionResultHolder.fail(stack);

        ResourceLocation originId = read(stack, ORIGIN_KEY);
        if (originId != null) {
            if (OriginRegistry.get(originId) == null) return InteractionResultHolder.fail(stack);
            OriginManager.chooseOrigin(player, layerId, originId, true);
        } else {
            OriginManager.removeOrigin(player, layerId);
            PlayerOriginsImpl state = PlayerOriginsAttachment.getOrCreate(player);
            state.setSelectingOrigin(true);
            OriginsServerNetwork.openChooseScreen(player, layer, true);
            OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        }
        consume(player, stack);
        return InteractionResultHolder.success(stack);
    }

    private static void consume(ServerPlayer player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    private static void describe(ItemStack stack, List<Component> lines) {
        ResourceLocation layerId = read(stack, LAYER_KEY);
        if (layerId == null) return;
        OriginLayer layer = OriginLayers.get(layerId);
        Component layerName = layer == null ? Component.literal(layerId.toString()) : layer.name();
        ResourceLocation originId = read(stack, ORIGIN_KEY);
        if (originId == null) {
            lines.add(Component.translatable("item.origins.orb_of_origin.layer_generic", layerName)
                .withStyle(ChatFormatting.GRAY));
            return;
        }
        Origin origin = OriginRegistry.get(originId);
        Component originName = origin == null ? Component.literal(originId.toString()) : origin.name();
        lines.add(Component.translatable("item.origins.orb_of_origin.layer_specific", layerName, originName)
            .withStyle(ChatFormatting.GRAY));
    }

    private static void writeData(ItemStack stack, CompoundTag tag) {
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(tag));
    }

    private static @Nullable CompoundTag readData(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data =
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        describe(stack, lines);
    }
}
