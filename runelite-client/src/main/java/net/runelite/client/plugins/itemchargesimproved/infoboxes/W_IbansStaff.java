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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerAnimation;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerChatMessage;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class W_IbansStaff extends ChargedItemInfoBox {
    public W_IbansStaff(
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
        super(ChargesItem.IBANS_STAFF, ItemID.IBANS_STAFF, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.ibans_staff;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.IBANS_STAFF),
            new TriggerItem(ItemID.IBANS_STAFF_1410),
            new TriggerItem(ItemID.IBANS_STAFF_U),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("You have (?<charges>.+) charges left on the staff."),
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(708).decreaseCharges(1).equipped()
        };
    }
}
