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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class J_NecklaceOfPassage extends ChargedItem {
    public J_NecklaceOfPassage(
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
        super(ChargesImprovedConfig.necklace_of_passage, ItemID.NECKLACE_OF_PASSAGE1, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.NECKLACE_OF_PASSAGE1).fixedCharges(1),
            new TriggerItem(ItemID.NECKLACE_OF_PASSAGE2).fixedCharges(2),
            new TriggerItem(ItemID.NECKLACE_OF_PASSAGE3).fixedCharges(3),
            new TriggerItem(ItemID.NECKLACE_OF_PASSAGE4).fixedCharges(4),
            new TriggerItem(ItemID.NECKLACE_OF_PASSAGE5).fixedCharges(5),
        };
    }
}
