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

public class J_SlayerRing extends ChargedItemInfoBox {
    public J_SlayerRing(
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
        super(ChargesItem.SLAYER_RING, ItemID.SLAYER_RING_8, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.triggers_items = new TriggerItem[]{
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
