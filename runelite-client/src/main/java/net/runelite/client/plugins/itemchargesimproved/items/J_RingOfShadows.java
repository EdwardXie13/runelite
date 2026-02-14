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

public class J_RingOfShadows extends ChargedItem {
    public J_RingOfShadows(
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
        super(ChargesImprovedConfig.ring_of_shadows, ItemID.RING_OF_SHADOWS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.RING_OF_SHADOWS_UNCHARGED).fixedCharges(0),
            new TriggerItem(ItemID.RING_OF_SHADOWS)
        };

        this.triggers = new TriggerBase[] {
            new OnChatMessage("Your ring of shadows has (?<charges>.+) charges? remaining.").setDynamicallyCharges(),

            // Charge.
            new OnChatMessage("You add (?<charges>.+) charges? to the ring of shadows.$").setDynamicallyCharges(),

            // Charge.
            new OnChatMessage("You add .+ charges? to the ring of shadows. It now has (?<charges>.+) charges?.").setDynamicallyCharges(),

            // TODO - teleport
        };
    }
}
