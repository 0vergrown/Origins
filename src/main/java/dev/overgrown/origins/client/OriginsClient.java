package dev.overgrown.origins.client;

import dev.overgrown.origins.Origins;
import dev.overgrown.origins.badge.CraftingRecipeTooltipData;
import dev.overgrown.origins.client.screen.ViewOriginScreen;
import dev.overgrown.origins.client.tooltip.CraftingRecipeClientTooltip;
import dev.overgrown.origins.item.OriginsItems;
import dev.overgrown.origins.network.OriginsClientNetwork;
import dev.overgrown.origins.network.payload.SwapCycleC2S;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.item.ItemProperties;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class OriginsClient implements ClientModInitializer {

    public static KeyMapping viewCurrentOriginKeybind;
    public static KeyMapping swapOriginKeybind;

    @Override
    public void onInitializeClient() {
        OriginsClientNetwork.register();

        TooltipComponentCallback.EVENT.register(data ->
            data instanceof CraftingRecipeTooltipData recipeData ? new CraftingRecipeClientTooltip(recipeData) : null);

        ItemProperties.register(OriginsItems.ORB_OF_ORIGIN, Origins.id("season"),
            (stack, level, entity, seed) -> Season.current().modelValue());

        viewCurrentOriginKeybind = new KeyMapping(
            "key.origins.view_origin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category." + Origins.MOD_ID);
        KeyBindingHelper.registerKeyBinding(viewCurrentOriginKeybind);

        swapOriginKeybind = new KeyMapping(
            "key.origins.swap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category." + Origins.MOD_ID);
        KeyBindingHelper.registerKeyBinding(swapOriginKeybind);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (viewCurrentOriginKeybind.consumeClick()) {
                if (!(Minecraft.getInstance().screen instanceof ViewOriginScreen)) {
                    Minecraft.getInstance().setScreen(new ViewOriginScreen());
                }
            }
            while (swapOriginKeybind.consumeClick()) {
                if (Minecraft.getInstance().screen != null) continue;
                if (!ClientPlayNetworking.canSend(SwapCycleC2S.TYPE)) continue;
                ClientPlayNetworking.send(new SwapCycleC2S(Screen.hasShiftDown()));
            }
        });
    }
}
