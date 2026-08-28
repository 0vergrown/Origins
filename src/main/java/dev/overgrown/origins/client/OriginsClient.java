package dev.overgrown.origins.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.CraftingRecipeTooltipData;
import dev.overgrown.origins.client.screen.ViewOriginScreen;
import dev.overgrown.origins.client.tooltip.CraftingRecipeClientTooltip;
import dev.overgrown.origins.item.OriginsItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Origins.MOD_ID, value = Dist.CLIENT)
public final class OriginsClient {

    public static KeyMapping viewCurrentOriginKeybind;
    public static KeyMapping swapOriginKeybind;

    private OriginsClient() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        viewCurrentOriginKeybind = new KeyMapping(
            "key.origins.view_origin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category." + Origins.MOD_ID);
        event.register(viewCurrentOriginKeybind);
        swapOriginKeybind = new KeyMapping(
            "key.origins.swap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category." + Origins.MOD_ID);
        event.register(swapOriginKeybind);
        NeoForge.EVENT_BUS.register(GameBus.class);
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CraftingRecipeTooltipData.class, CraftingRecipeClientTooltip::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(OriginsItems.ORB_OF_ORIGIN.get(), Origins.id("season"),
            (stack, level, entity, seed) -> Season.current().modelValue()));
        dev.overgrown.origins.origin.OriginManager.setClientHolding((player, layerId, originId) -> {
            java.util.UUID uuid = player.getUUID();
            if (originId.equals(OriginsClientState.get(uuid).get(layerId))) return true;
            java.util.List<net.minecraft.resources.ResourceLocation> pool =
                OriginsClientState.getPool(uuid).get(layerId);
            return pool != null && pool.contains(originId);
        });
    }

    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            OriginRollQueue.clear();
            dev.overgrown.origins.origin.OriginCaps.clear();
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            OriginRollQueue.tick(Minecraft.getInstance());
            if (viewCurrentOriginKeybind == null) return;
            while (viewCurrentOriginKeybind.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (!(mc.screen instanceof ViewOriginScreen)) {
                    mc.setScreen(new ViewOriginScreen());
                }
            }
            if (swapOriginKeybind == null) return;
            while (swapOriginKeybind.consumeClick()) {
                if (Minecraft.getInstance().screen != null) continue;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new dev.overgrown.origins.network.payload.SwapCycleC2S(
                        net.minecraft.client.gui.screens.Screen.hasShiftDown()));
            }
        }
    }
}
