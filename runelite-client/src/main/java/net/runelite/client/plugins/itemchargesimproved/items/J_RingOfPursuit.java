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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.HitsplatTarget;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class J_RingOfPursuit extends ChargedItem {
    public J_RingOfPursuit(
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
        super(ChargesImprovedConfig.ring_of_pursuit, ItemID.RING_OF_PURSUIT, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.RING_OF_PURSUIT).needsToBeEquipped(),
        };

        this.triggers = new TriggerBase[]{
            // Check.
            new OnChatMessage("Your ring of pursuit has (?<charges>.+) charges? left.").setDynamicallyCharges(),

            // Use.
            new OnChatMessage("Your ring of pursuit reveals the entire trail to you. It has (?<charges>.+) charges? left.").setDynamicallyCharges(),

            // Use last charge.
            new OnChatMessage("Your ring of pursuit reveals the entire trail to you. It then crumbles to dust.").notification().setFixedCharges(10),

            // Destroy.
            new OnChatMessage("The ring shatters. Your next ring of pursuit will start afresh from 10 charges.").setFixedCharges(10),
        };
    }
}
