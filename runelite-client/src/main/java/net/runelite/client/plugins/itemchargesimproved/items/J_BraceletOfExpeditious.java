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
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class J_BraceletOfExpeditious extends ChargedItem {
    public J_BraceletOfExpeditious(
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
        super(ChargesImprovedConfig.bracelet_of_expeditious, ItemID.EXPEDITIOUS_BRACELET, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.EXPEDITIOUS_BRACELET).needsToBeEquipped(),
        };

        this.triggers = new TriggerBase[] {
            // Check.
            new OnChatMessage("Your expeditious bracelet has (?<charges>.+) charges? left.").setDynamicallyCharges(),

            // Charge used.
            new OnChatMessage("Your expeditious bracelet helps you progress your slayer( task)? faster. It has (?<charges>.+) charges? left.").setDynamicallyCharges(),

            // Bracelet fully used.
            new OnChatMessage("Your expeditious bracelet helps you progress your slayer faster. It then crumbles to dust.").setFixedCharges(30).notification("Your expeditious bracelet crumbles to dust."),

            // Break.
            new OnChatMessage("The bracelet shatters. Your next expeditious bracelet will start afresh from (?<charges>.+) charges.").setDynamicallyCharges(),
        };
    }
}
