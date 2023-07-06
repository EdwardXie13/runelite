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

public class BarrowsToragsHelm extends ChargedItemInfoBox {
    public BarrowsToragsHelm(
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
        super(ChargesItem.BARROWS_GEAR, ItemID.TORAGS_HELM, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.charges = 100;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.TORAGS_HELM).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_HELM_100).fixedCharges(100),
            new TriggerItem(ItemID.TORAGS_HELM_75).fixedCharges(75),
            new TriggerItem(ItemID.TORAGS_HELM_50).fixedCharges(50),
            new TriggerItem(ItemID.TORAGS_HELM_25).fixedCharges(25),
            new TriggerItem(ItemID.TORAGS_HELM_0).fixedCharges(0)
        };
    }
}