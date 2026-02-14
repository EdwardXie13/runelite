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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuEntryAdded;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class S_KharedstMemoirs extends ChargedItem {
    public S_KharedstMemoirs(
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
        super(ChargesImprovedConfig.kharedsts_memoirs, ItemID.KHAREDSTS_MEMOIRS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.KHAREDSTS_MEMOIRS),
            new TriggerItem(ItemID.BOOK_OF_THE_DEAD)
        };

        this.triggers = new TriggerBase[] {
            new OnChatMessage("You add an entry to Kharedst's Memoirs.").increaseCharges(20),

            // Teleport.
            new OnChatMessage("((Kharedst's Memoirs)|(The Book of the Dead)) now has (?<charges>.+) (memories|memory) remaining.").setDynamicallyCharges(),

            // Check empty.
            new OnChatMessage("((Kharedst's Memoirs)|(The Book of the Dead)) holds no charges.").setFixedCharges(0),

            // Check.
            new OnChatMessage("On the inside of the cover a message is displayed in dark ink. It reads: (?<charges>.+)/.+? (memories|memory) remain.").setDynamicallyCharges(),

            // Charge.
            new OnChatMessage("((Kharedst's Memoirs)|(The Book of the Dead)) now has (?<charges>.+) charges.").setDynamicallyCharges(),

            // Try to charge book of the dead when already full.
            new OnChatMessage("The Book of the Dead is already fully charged.").setFixedCharges(250),

            // Try to charge kharedst memoirs when already full.
            new OnChatMessage("Kharedst's Memoirs is already fully charged.").setFixedCharges(100),

            // Common menu entries.
            new OnMenuEntryAdded("Reminisce").replaceOption("Teleport"),

            // Hide destroy.
            new OnMenuEntryAdded("Destroy").hide(),
        };
    }
}
