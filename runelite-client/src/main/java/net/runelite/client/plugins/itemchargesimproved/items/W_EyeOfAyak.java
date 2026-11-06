package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.*;
import net.runelite.client.plugins.itemchargesimproved.store.Store;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class W_EyeOfAyak extends ChargedItem {
    public W_EyeOfAyak(
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
        super(ChargesImprovedConfig.eye_of_ayak, ItemID.EYE_OF_AYAK, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.EYE_OF_AYAK_UNCHARGED).fixedCharges(0),
            new TriggerItem(ItemID.EYE_OF_AYAK),
        };

        this.triggers = new TriggerBase[] {
            // Check.
            new OnChatMessage("The Eye of Ayak has been charged.*? It currently has (?<charges>[\\d,]+) charges").setDynamicallyCharges(),

            // Uncharge.
            new OnChatMessage("You uncharge the Eye of Ayak").setFixedCharges(0),

            // Attack.
            new OnAnimationChanged(12397).decreaseCharges(1),
        };
    }
}
