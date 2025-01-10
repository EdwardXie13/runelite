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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuEntryAdded;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class W_EnchantedLyre extends ChargedItem {
    public W_EnchantedLyre(
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
        super(ChargesImprovedConfig.enchanted_lyre, ItemID.ENCHANTED_LYRE, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.LYRE).fixedCharges(0),
            new TriggerItem(ItemID.ENCHANTED_LYRE).fixedCharges(0),
            new TriggerItem(ItemID.ENCHANTED_LYRE1).fixedCharges(1),
            new TriggerItem(ItemID.ENCHANTED_LYRE2).fixedCharges(2),
            new TriggerItem(ItemID.ENCHANTED_LYRE3).fixedCharges(3),
            new TriggerItem(ItemID.ENCHANTED_LYRE4).fixedCharges(4),
            new TriggerItem(ItemID.ENCHANTED_LYRE5).fixedCharges(5),
        };

        this.triggers = new TriggerBase[]{
            new OnMenuEntryAdded("Play").replaceOption("Teleport"),
        };
    }
}
