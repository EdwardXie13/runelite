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

public class U_Waterskin extends ChargedItemInfoBox {
    public U_Waterskin(
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
        super(ChargesItem.WATERSKIN, ItemID.WATERSKIN0, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.waterskin;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.WATERSKIN0).fixedCharges(0),
            new TriggerItem(ItemID.WATERSKIN1).fixedCharges(1),
            new TriggerItem(ItemID.WATERSKIN2).fixedCharges(2),
            new TriggerItem(ItemID.WATERSKIN3).fixedCharges(3),
            new TriggerItem(ItemID.WATERSKIN4).fixedCharges(4),
        };
    }
}
