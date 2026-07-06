package dev.overgrown.origins.item;

import dev.overgrown.origins.Origins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


@EventBusSubscriber(modid = Origins.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class OriginsItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Origins.MOD_ID);

    public static final Supplier<OrbOfOriginItem> ORB_OF_ORIGIN =
        ITEMS.register("orb_of_origin", OrbOfOriginItem::new);

    private OriginsItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ORB_OF_ORIGIN.get());
        }
    }
}
