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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnGraphicChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnResetDaily;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Charges;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class B_FremennikSeaBoots extends ChargedItem {
    public B_FremennikSeaBoots(
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
        super(ChargesImprovedConfig.fremennik_sea_boots, ItemID.FREMENNIK_SEA_BOOTS_1, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.FREMENNIK_SEA_BOOTS_1),
            new TriggerItem(ItemID.FREMENNIK_SEA_BOOTS_2),
            new TriggerItem(ItemID.FREMENNIK_SEA_BOOTS_3),
            new TriggerItem(ItemID.FREMENNIK_SEA_BOOTS_4).fixedCharges(Charges.UNLIMITED),
        };

        this.triggers = new TriggerBase[]{
            // Try to teleport while empty.
            new OnChatMessage("You have already used your available teleport for today. Try again tomorrow when the boots have recharged.").setFixedCharges(0),

            // Teleport.
            new OnGraphicChanged(111).onItemClick().decreaseCharges(1),

            // Daily reset.
            new OnResetDaily().specificItem(ItemID.FREMENNIK_SEA_BOOTS_1).setFixedCharges(1),
            new OnResetDaily().specificItem(ItemID.FREMENNIK_SEA_BOOTS_2).setFixedCharges(1),
            new OnResetDaily().specificItem(ItemID.FREMENNIK_SEA_BOOTS_3).setFixedCharges(1),
        };
    }
}
