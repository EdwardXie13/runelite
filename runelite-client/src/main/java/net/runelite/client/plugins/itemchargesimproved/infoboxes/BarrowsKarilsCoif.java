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

public class BarrowsKarilsCoif extends ChargedItemInfoBox {
    public BarrowsKarilsCoif(
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
        super(ChargesItem.BARROWS_GEAR, ItemID.KARILS_COIF, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.charges = 100;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.KARILS_COIF).fixedCharges(100),
            new TriggerItem(ItemID.KARILS_COIF_100).fixedCharges(100),
            new TriggerItem(ItemID.KARILS_COIF_75).fixedCharges(75),
            new TriggerItem(ItemID.KARILS_COIF_50).fixedCharges(50),
            new TriggerItem(ItemID.KARILS_COIF_25).fixedCharges(25),
            new TriggerItem(ItemID.KARILS_COIF_0).fixedCharges(0)
        };
    }
}