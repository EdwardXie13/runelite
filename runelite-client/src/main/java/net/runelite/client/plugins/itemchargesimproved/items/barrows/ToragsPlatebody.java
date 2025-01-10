package net.runelite.client.plugins.itemchargesimproved.items.barrows;

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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class ToragsPlatebody extends ChargedItem {
    public ToragsPlatebody(
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
        super(ChargesImprovedConfig.barrows_gear, ItemID.TORAGS_PLATEBODY, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.TORAGS_PLATEBODY).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_PLATEBODY_100).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_PLATEBODY_75).fixedCharges(75),
            new TriggerItem(ItemID.TORAGS_PLATEBODY_50).fixedCharges(50),
            new TriggerItem(ItemID.TORAGS_PLATEBODY_25).fixedCharges(25),
            new TriggerItem(ItemID.TORAGS_PLATEBODY_0).fixedCharges(0)
        };
    }
}