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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerChatMessage;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class W_ToxicStaffOfTheDead extends ChargedItemInfoBox {
    public W_ToxicStaffOfTheDead(
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
        super(ChargesItem.TOXIC_STAFF_OF_THE_DEAD, ItemID.TOXIC_STAFF_OF_THE_DEAD, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.toxic_staff_of_the_dead;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.TOXIC_STAFF_UNCHARGED).fixedCharges(0),
            new TriggerItem(ItemID.TOXIC_STAFF_OF_THE_DEAD)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Scales: (?<charges>.+)"),
        };
    }
}
