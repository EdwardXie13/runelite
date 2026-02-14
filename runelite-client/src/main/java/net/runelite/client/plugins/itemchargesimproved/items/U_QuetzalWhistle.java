package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnAnimationChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnItemContainerChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuEntryAdded;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.ItemContainerId;
import net.runelite.client.plugins.itemchargesimproved.store.ItemWithQuantity;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class U_QuetzalWhistle extends ChargedItem {
    public U_QuetzalWhistle(
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Store store,
        final Gson gson
    ) {
        super(ChargesImprovedConfig.quetzal_whistle, ItemID.BASIC_QUETZAL_WHISTLE, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.BASIC_QUETZAL_WHISTLE).maxCharges(5),
            new TriggerItem(ItemID.ENHANCED_QUETZAL_WHISTLE).maxCharges(20),
            new TriggerItem(ItemID.PERFECTED_QUETZAL_WHISTLE).maxCharges(50),
        };

        this.triggers = new TriggerBase[] {
            // Check.
            new OnChatMessage("Your quetzal whistle has (?<charges>.+) charges? remaining.").setDynamicallyCharges(),

            // Teleport.
            new OnAnimationChanged(10944).decreaseCharges(1),

            // Teleport menu entry.
            new OnMenuEntryAdded("Signal").replaceOption("Teleport"),

            // Craft basic quetzal whistle.
            new OnChatMessage("You craft yourself a basic quetzal whistle.").setFixedCharges(0),

            // Fully charged.
            new OnChatMessage("Looks like the birds are all full for now. Make them work a bit before feeding them again!").requiredItem(ItemID.BASIC_QUETZAL_WHISTLE).setFixedCharges(5),
            new OnChatMessage("Looks like the birds are all full for now. Make them work a bit before feeding them again!").requiredItem(ItemID.ENHANCED_QUETZAL_WHISTLE).setFixedCharges(20),
            new OnChatMessage("Looks like the birds are all full for now. Make them work a bit before feeding them again!").requiredItem(ItemID.PERFECTED_QUETZAL_WHISTLE).setFixedCharges(50),

            // Partially charged.
            new OnItemContainerChanged(ItemContainerId.INVENTORY).hasChatMessage("Soar Leader Pitri|There you go. Some whistle charges for you!").onItemContainerDifference(itemsDifference -> {
                for (final ItemWithQuantity item : itemsDifference.items) {
                    switch (item.itemId) {
                        case ItemID.QUETZAL_FEED:
                        case ItemID.RAW_WILD_KEBBIT:
                        case ItemID.RAW_BARBTAILED_KEBBIT:
                        case ItemID.RAW_LARUPIA:
                            increaseCharges(Math.abs(item.quantity));
                            break;
                        case ItemID.RAW_GRAAHK:
                        case ItemID.RAW_KYATT:
                        case ItemID.RAW_PYRE_FOX:
                            increaseCharges(Math.abs(item.quantity) * 2);
                            break;
                        case ItemID.RAW_DASHING_KEBBIT:
                        case ItemID.RAW_SUNLIGHT_ANTELOPE:
                        case ItemID.RAW_MOONLIGHT_ANTELOPE:
                            increaseCharges(Math.abs(item.quantity) * 3);
                            break;
                    }
                }
            })
        };
    }
}
