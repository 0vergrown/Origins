package dev.overgrown.origins.item;

import dev.overgrown.origins.Origins;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class OriginsItems {
    public static final Item ORB_OF_ORIGIN = new OrbOfOriginItem();

    private OriginsItems() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, Origins.id("orb_of_origin"), ORB_OF_ORIGIN);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(content -> content.accept(ORB_OF_ORIGIN));
    }
}
