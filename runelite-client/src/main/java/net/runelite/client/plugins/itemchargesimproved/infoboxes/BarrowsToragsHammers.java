package net.runelite.client.plugins.itemchargesimproved.infoboxes;

import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.itemchargesimproved.ChargedItemInfoBox;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.ChargesItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class BarrowsToragsHammers extends ChargedItemInfoBox {
    public BarrowsToragsHammers(
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Plugin plugin
    ) {
        super(ChargesItem.BARROWS_GEAR, ItemID.TORAGS_HAMMERS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.charges = 100;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.TORAGS_HAMMERS).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_HAMMERS_100).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_HAMMERS_75).fixedCharges(75),
            new TriggerItem(ItemID.TORAGS_HAMMERS_50).fixedCharges(50),
            new TriggerItem(ItemID.TORAGS_HAMMERS_25).fixedCharges(25),
            new TriggerItem(ItemID.TORAGS_HAMMERS_0).fixedCharges(0)
        };
    }
}