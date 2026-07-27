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

    private OriginsClient() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        viewCurrentOriginKeybind = new KeyMapping(
            "key.origins.view_origin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category." + Origins.MOD_ID);
        event.register(viewCurrentOriginKeybind);
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
    }

    public static final class GameBus {
        private GameBus() {}

        @SubscribeEvent
        public static void onLoggingOut(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            OriginRollQueue.clear();
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
        }
    }
}
