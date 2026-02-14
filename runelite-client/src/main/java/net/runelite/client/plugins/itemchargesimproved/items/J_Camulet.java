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
import net.runelite.client.plugins.itemchargesimproved.store.Charges;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class J_Camulet extends ChargedItem {
    public J_Camulet(
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
        super(ChargesImprovedConfig.camulet, ItemID.CAMULET, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.CAMULET),
        };

        this.triggers = new TriggerBase[] {
            // Check.
            new OnChatMessage("Your Camulet has one charge left.").setFixedCharges(1),
            new OnChatMessage("Your Camulet has (?<charges>.+) charges left.").setDynamicallyCharges(),

            // Recharge.
            new OnChatMessage("You recharge the Camulet using camel dung. Yuck!").setFixedCharges(4),

            // Trying to charge fully charged.
            new OnChatMessage("The Camulet is already fully charged.").setFixedCharges(4),

            // Unlimited charges.
            new OnChatMessage("The Camulet has unlimited charges.").setFixedCharges(Charges.UNLIMITED),

            // Replace check.
            new OnMenuEntryAdded("Check-charge").replaceOption("Check"),
        };
    }
}
