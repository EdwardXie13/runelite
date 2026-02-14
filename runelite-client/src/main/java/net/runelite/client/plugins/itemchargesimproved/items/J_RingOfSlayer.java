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

public class J_RingOfSlayer extends ChargedItem {
    public J_RingOfSlayer(
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
        super(ChargesImprovedConfig.slayer_ring, ItemID.SLAYER_RING_8, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.SLAYER_RING_1).fixedCharges(1),
            new TriggerItem(ItemID.SLAYER_RING_2).fixedCharges(2),
            new TriggerItem(ItemID.SLAYER_RING_3).fixedCharges(3),
            new TriggerItem(ItemID.SLAYER_RING_4).fixedCharges(4),
            new TriggerItem(ItemID.SLAYER_RING_5).fixedCharges(5),
            new TriggerItem(ItemID.SLAYER_RING_6).fixedCharges(6),
            new TriggerItem(ItemID.SLAYER_RING_7).fixedCharges(7),
            new TriggerItem(ItemID.SLAYER_RING_8).fixedCharges(8),
        };
    }
}
